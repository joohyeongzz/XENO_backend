package com.daewon.xeno_backend.service;


import com.daewon.xeno_backend.domain.*;
import com.daewon.xeno_backend.domain.auth.Users;
import com.daewon.xeno_backend.dto.order.OrderCancelDTO;
import com.daewon.xeno_backend.dto.product.ProductRegisterDTO;
import com.daewon.xeno_backend.dto.product.ProductSizeDTO;
import com.daewon.xeno_backend.repository.DeliveryTrackRepository;
import com.daewon.xeno_backend.repository.OrdersRefundRepository;
import com.daewon.xeno_backend.repository.OrdersRepository;
import com.daewon.xeno_backend.repository.Products.ProductsImageRepository;
import com.daewon.xeno_backend.repository.Products.ProductsOptionRepository;
import com.daewon.xeno_backend.repository.Products.ProductsBrandRepository;
import com.daewon.xeno_backend.repository.auth.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.poi.ss.usermodel.CellType.STRING;

@Service
@Log4j2
@RequiredArgsConstructor
public class ExcelService {

    private final ProductsImageRepository productsImageRepository;
    private final ProductsOptionRepository productsOptionRepository;
    private final ProductsBrandRepository productsBrandRepository;
    private final UserRepository userRepository;
    private final OrdersRepository ordersRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper jacksonObjectMapper;
    private final DeliveryTrackRepository deliveryTrackRepository;
    private final OrdersRefundRepository ordersRefundRepository;
    private final OrdersService ordersService;

    // 엑셀 파일 읽기
    public List<ProductRegisterDTO> parseExcelFile(MultipartFile excel) throws IOException {
        List<ProductRegisterDTO> productList = new ArrayList<>();

        try (InputStream fis = excel.getInputStream(); Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0 || row.getCell(0) == null
                       ) continue; // 헤더 스킵, 품번 null이면 스킵
                int rowIndex = 0; // 반복 횟수를 추적하기 위한 변수 선언
                ProductRegisterDTO product = new ProductRegisterDTO();
                log.info(rowIndex+"번쨰"+row.getCell(0));

                Cell cell = row.getCell(0); // A열 품번
                String productNumber = "";

                if (cell != null) { // 품번이 null 이 아닌 경우
                    switch (cell.getCellType()) {
                        case STRING:
                            productNumber = cell.getStringCellValue();
                            if(productNumber.isEmpty()) { // 품번이 공백일 경우 스킵
                                continue;
                            }
                            break;
                        case NUMERIC:
                            // 숫자일 경우 String으로 변경
                            log.info(productNumber);
                            productNumber = String.valueOf((long)cell.getNumericCellValue());
                            break;
                        case BLANK:
                            continue;
                        default:
                            productNumber = "";
                            break;
                    }
                }

                // 각 열에 해당하는 정보를 DTD에 매핑
                product.setProductNumber(productNumber);
                product.setName(row.getCell(1).getStringCellValue());
                product.setCategory(row.getCell(2).getStringCellValue());
                product.setCategorySub(row.getCell(3).getStringCellValue());
                product.setPrice((long)row.getCell(4).getNumericCellValue());
                product.setPriceSale(row.getCell(5) == null || row.getCell(5).getNumericCellValue() == 0 ? 0 : (long)row.getCell(5).getNumericCellValue());
                product.setSale(product.getPriceSale() != 0);

                product.setColors(row.getCell(6).getStringCellValue());
                String sizeString = getCellValue(row.getCell(7)); // Comma-separated sizes
                String stockString = getCellValue(row.getCell(8)); // Comma-separated stocks
                log.info(stockString);

                // 사이즈와 재고는 ,를 구분자로 새 배열 생성
                String[] sizes = sizeString.split(",");

                String[] stocks = stockString.split(",");

                if (sizes.length != stocks.length) {
                    // 로깅 또는 오류 처리
                    System.err.println("Sizes and stocks arrays must have the same length for row " + row.getRowNum());
                    continue; // 다음 행으로 넘어갑니다.
                }

                List<ProductSizeDTO> sizeDTOs = new ArrayList<>();
                for (int i = 0; i < sizes.length; i++) {
                    ProductSizeDTO sizeDTO = new ProductSizeDTO();
                    sizeDTO.setSize(sizes[i].trim());

                    try {
                        // 소수점을 포함한 문자열을 Double로 변환한 후 정수로 변환
                        double stockDouble = Double.parseDouble(stocks[i].trim());
                        int stockInt = (int) Math.round(stockDouble);
                        sizeDTO.setStock(stockInt);
                    } catch (NumberFormatException e) {
                        // 숫자 변환 오류 처리
                        System.err.println("Error parsing stock value: " + stocks[i]);
                        sizeDTO.setStock(0); // 기본값 또는 오류 처리 로직
                    }

                    sizeDTOs.add(sizeDTO);
                }

                product.setSize(sizeDTOs);

                product.setUrl_1(row.getCell(9).getStringCellValue());

                Cell cell12 = row.getCell(10);
                Cell cell13 = row.getCell(11);
                Cell cell14 = row.getCell(12);
                Cell cell15 = row.getCell(13);
                Cell cell16 = row.getCell(14);

// 각 셀의 값을 안전하게 읽어오기
                String url2 = (cell12 != null) ? cell12.getStringCellValue() : null;
                String url3 = (cell13 != null) ? cell13.getStringCellValue() : null;
                String url4 = (cell14 != null) ? cell14.getStringCellValue() : null;
                String url5 = (cell15 != null) ? cell15.getStringCellValue() : null;
                String url6 = (cell16 != null) ? cell16.getStringCellValue() : null;

// 제품 객체에 값 설정
                product.setUrl_2(url2);
                product.setUrl_3(url3);
                product.setUrl_4(url4);
                product.setUrl_5(url5);
                product.setUrl_6(url6);
                product.setDetail_url(row.getCell(15).getStringCellValue());
                product.setSeason(row.getCell(16).getStringCellValue());

                productList.add(product);
            }
        }

        return productList;
    }

    private String getCellValue(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            default:
                return "";
        }
    }

    // 내 상품 엑셀 다운로드
    public byte[] generateExcelFile() throws IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserName = authentication.getName();
        Optional<Users> optionalUser = userRepository.findByEmail(currentUserName);
        if (optionalUser.isEmpty()) {
            throw new RuntimeException("User not found: " + currentUserName);
        }

        Users users = optionalUser.get(); // 유저 찾기

        List<ProductsBrand> products = productsBrandRepository.findByBrand(users.getBrand());

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");
        sheet.setDefaultColumnWidth(10);

        // Header data
        int rowCount = 0;
        String headerNames[] = new String[]{
                "품번",
                "상품 이름",
                "카테고리",
                "서브 카테고리",
                "가격",
                "할인 가격",
                "색상",
                "사이즈",
                "재고",
                "이미지1",
                "이미지2",
                "이미지3",
                "이미지4",
                "이미지5",
                "이미지6",
                "상세이미지",
                "시즌"
        };

        Row headerRow = sheet.createRow(rowCount++);
        for (int i = 0; i < headerNames.length; i++) {
            Cell headerCell = headerRow.createCell(i);
            headerCell.setCellValue(headerNames[i]);
        }

        CellStyle textStyle = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        textStyle.setDataFormat(format.getFormat("@")); // Text format

        for (int rowIndex = 1; rowIndex <= 100; rowIndex++) { // 예를 들어 100행 정도를 처리
            Row row = sheet.createRow(rowIndex); // 행 생성
            Cell stockCell = row.createCell(8); // '재고' 열 셀 생성
            stockCell.setCellStyle(textStyle); // 텍스트 서식 적용, 적용 안할 시 나중에 엑셀 등록할 때 오류 생김
        }
        // 카테고리와 서브 카테고리 데이터
        String[] categories = {"상의", "하의", "아우터", "액세서리"};

        // 카테고리 드롭다운 설정
        DataValidationHelper validationHelper = sheet.getDataValidationHelper();
        DataValidationConstraint categoryConstraint = validationHelper.createExplicitListConstraint(categories);
        CellRangeAddressList categoryAddressList = new CellRangeAddressList(1, 100, 2, 2); // C열에 적용
        DataValidation categoryValidation = validationHelper.createValidation(categoryConstraint, categoryAddressList);
        categoryValidation.setShowErrorBox(true);
        sheet.addValidationData(categoryValidation);

        int rowIndex = 1;
        for (ProductsBrand product : products) {
            Row row = sheet.createRow(rowIndex++);
            // 셀 만들고 엔티티의 각 컬럼들을 매핑
            row.createCell(0).setCellValue(product.getProducts().getProductNumber());
            row.createCell(1).setCellValue(product.getProducts().getName());
            row.createCell(2).setCellValue(product.getProducts().getCategory());
            row.createCell(3).setCellValue(product.getProducts().getCategorySub());
            row.createCell(4).setCellValue(product.getProducts().getPrice());
            row.createCell(5).setCellValue(product.getProducts().getPriceSale() != 0 ? product.getProducts().getPriceSale() : 0);
            row.createCell(6).setCellValue(product.getProducts().getColor());

            List<ProductsOption> productsOptions = productsOptionRepository.findByProductId(product.getProducts().getProductId());

            // 재고 배열 및 사이즈 배열의 각 배열 사이 ,를 넣어 하나의 String으로 저장
            String sizes = productsOptions.stream()
                    .map(ProductsOption::getSize)
                    .collect(Collectors.joining(","));
            String stocks = productsOptions.stream()
                    .map(option -> String.valueOf(option.getStock()))
                    .collect(Collectors.joining(","));

            row.createCell(7).setCellValue(sizes);
            row.createCell(8).setCellValue(stocks);

            ProductsImage productsImage = productsImageRepository.findByProductId(product.getProducts().getProductId());

            row.createCell(9).setCellValue(productsImage.getUrl_1() == null ? "" : productsImage.getUrl_1());
            row.createCell(10).setCellValue(productsImage.getUrl_2() == null ? "" : productsImage.getUrl_2());
            row.createCell(11).setCellValue(productsImage.getUrl_3() == null ? "" : productsImage.getUrl_3());
            row.createCell(12).setCellValue(productsImage.getUrl_4() == null ? "" : productsImage.getUrl_4());
            row.createCell(13).setCellValue(productsImage.getUrl_5() == null ? "" : productsImage.getUrl_5());
            row.createCell(14).setCellValue(productsImage.getUrl_6() == null ? "" : productsImage.getUrl_6());
            row.createCell(15).setCellValue(productsImage.getDetail_url() == null ? "" : productsImage.getDetail_url());
            row.createCell(16).setCellValue(product.getProducts().getSeason());

        }

        // Write to ByteArrayOutputStream
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            workbook.close();
            return outputStream.toByteArray();
        }
    }

    // 새 액셀 템플릿 다운
    public byte[] newGenerateExcelFile() throws IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserName = authentication.getName();
        Optional<Users> optionalUser = userRepository.findByEmail(currentUserName);
        if (optionalUser.isEmpty()) {
            throw new RuntimeException("User not found: " + currentUserName);
        }

        Users users = optionalUser.get();

        List<ProductsBrand> products = productsBrandRepository.findByBrand(users.getBrand());

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");
        sheet.setDefaultColumnWidth(10);

        // Header data
        int rowCount = 0;
        String headerNames[] = new String[]{
                "품번",
                "상품 이름",
                "카테고리",
                "서브 카테고리",
                "가격",
                "할인 가격",
                "색상",
                "사이즈",
                "재고",
                "이미지1",
                "이미지2",
                "이미지3",
                "이미지4",
                "이미지5",
                "이미지6",
                "상세이미지",
                "시즌"
        };

        Row headerRow = sheet.createRow(rowCount++);
        for (int i = 0; i < headerNames.length; i++) {
            Cell headerCell = headerRow.createCell(i);
            headerCell.setCellValue(headerNames[i]);
        }

        CellStyle textStyle = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        textStyle.setDataFormat(format.getFormat("@")); // Text format

        for (int rowIndex = 1; rowIndex <= 100; rowIndex++) { // 예를 들어 100행 정도를 처리
            Row row = sheet.createRow(rowIndex); // 행 생성
            Cell stockCell = row.createCell(8); // '재고' 열 셀 생성
            stockCell.setCellStyle(textStyle); // 텍스트 서식 적용
        }

        // Write to ByteArrayOutputStream
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            workbook.close();
            return outputStream.toByteArray();
        }
    }


    // 재고 엑셀 다운
    public byte[] generateStockExcelFile() throws IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserName = authentication.getName();
        Optional<Users> optionalUser = userRepository.findByEmail(currentUserName);
        if (optionalUser.isEmpty()) {
            throw new RuntimeException("User not found: " + currentUserName);
        }

        Users users = optionalUser.get();

        List<ProductsBrand> products = productsBrandRepository.findByBrand(users.getBrand());

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");
        sheet.setDefaultColumnWidth(10);


        int rowCount = 0;
        String[] headerNames = new String[]{
                "상품 코드",
                "품번",
                "상품 이름",
                "사이즈",
                "재고"
        };

        Row headerRow = sheet.createRow(rowCount++);
        for (int i = 0; i < headerNames.length; i++) {
            Cell headerCell = headerRow.createCell(i);
            headerCell.setCellValue(headerNames[i]);
        }

        int rowIndex = 1;
        for (ProductsBrand product : products) {
            List<ProductsOption> productsOptions = productsOptionRepository.findByProductId(product.getProducts().getProductId());
            for (ProductsOption productsOption : productsOptions) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(productsOption.getProductOptionId());
                row.createCell(1).setCellValue(product.getProducts().getProductNumber());
                row.createCell(2).setCellValue(product.getProducts().getName());
                row.createCell(3).setCellValue(productsOption.getSize());
                row.createCell(4).setCellValue(productsOption.getStock());
            }
        }


        // 재고 제외 수정 못하게 잠금 걸기

        CellStyle lockedCellStyle = workbook.createCellStyle();
        lockedCellStyle.setLocked(true);

        for (Row row : sheet) {
            if (row.getRowNum() > 0) { // Skip header row
                for (int colIndex = 0; colIndex <= 3; colIndex++) { // Protect columns 0, 1, 2, 3
                    Cell cell = row.getCell(colIndex);
                    if (cell != null) {
                        cell.setCellStyle(lockedCellStyle);
                    }
                }
            }
        }

        CellStyle unlockedCellStyle = workbook.createCellStyle();
        unlockedCellStyle.setLocked(false); // Allow editing
        for (Row row : sheet) {
            if (row.getRowNum() > 0) { // Skip header row
                Cell stockCell = row.getCell(4);
                if (stockCell != null) {
                    stockCell.setCellStyle(unlockedCellStyle);
                }
            }
        }

        DataValidationHelper validationHelper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = validationHelper.createNumericConstraint(
                DataValidationConstraint.ValidationType.INTEGER,
                DataValidationConstraint.OperatorType.BETWEEN,
                "0",
                "1000000"
        );

        CellRangeAddressList addressList = new CellRangeAddressList(1, sheet.getLastRowNum(), 4, 4); // Apply to entire column E
        DataValidation validation = validationHelper.createValidation(constraint, addressList);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);


        sheet.protectSheet("password");


        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            workbook.close();
            return outputStream.toByteArray();
        }
    }

    // 재고 수정 엑셀 업로드
    @Transactional
    public void parseStockExcelFile(MultipartFile excel) throws IOException {
        if (excel.isEmpty()) {
            throw new RuntimeException("업로드된 파일이 비어 있습니다.");
        }

        try (InputStream fis = excel.getInputStream(); Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);

            if (sheet == null) {
                throw new RuntimeException("엑셀 시트가 존재하지 않습니다.");
            }

            // 헤더 행 검사
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new RuntimeException("헤더 행이 존재하지 않습니다.");
            }

            // 헤더 셀 검사 (필요한 열 개수에 따라 수정)
            for (int i = 0; i <= 4; i++) {
                Cell headerCell = headerRow.getCell(i);
                if (headerCell == null || headerCell.getStringCellValue().trim().isEmpty()) {
                    throw new RuntimeException("헤더 셀이 비어 있습니다. 셀 인덱스: " + i);
                }
            }

            for (Row row : sheet) {
                // 첫 번째 행(헤더)은 건너뜀
                if (row.getRowNum() == 0) continue;

                // 비어 있는 행 건너뜀
                if (row.getCell(0) == null || row.getCell(0).getCellType() == STRING) {
                    throw new RuntimeException("Product option ID not found in row " + row.getRowNum());
                }

                Cell cell = row.getCell(0);
                if (cell == null) {
                    throw new RuntimeException("Product option ID not found in row " + row.getRowNum());
                } else {
                    try {
                        long productOptionId = (long) cell.getNumericCellValue(); // 상품 옵션 ID 불러오기
                        ProductsOption productsOption = productsOptionRepository.findById(productOptionId).orElse(null);

                        if (productsOption != null) {
                            Cell stockCell = row.getCell(4);
                            if (stockCell != null) {
                                productsOption.setStock((long) stockCell.getNumericCellValue());
                                productsOptionRepository.save(productsOption);
                            } else {
                                throw new RuntimeException("Stock value not found in row " + row.getRowNum());
                            }
                        } else {
                            throw new RuntimeException("Product option with ID " + productOptionId + " not found.");
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Error processing row " + row.getRowNum() + ": " + e.getMessage(), e);
                    }
                }
            }
        }
    }

    // 결제 완료 엑셀 다운 받기
    public byte[] generateOrdersExcelFile() throws IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserName = authentication.getName();
        Optional<Users> optionalUser = userRepository.findByEmail(currentUserName);

        if (optionalUser.isEmpty()) {
            throw new RuntimeException("User not found: " + currentUserName);
        }

        Users users = optionalUser.get(); // 유저 찾기

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");
        sheet.setDefaultColumnWidth(10);

        // Header data
        int rowCount = 0;
        String headerNames[] = new String[]{
                "주문 번호",
                "결제 시간",
                "결제 금액",
                "수량",
                "요청 사항",
                "배송 상태",
                "품번",
                "상품 이름",
                "색상",
                "사이즈",
                "구매자",
                "택배사",
                "운송장 번호"
        };

        Row headerRow = sheet.createRow(rowCount++);
        for (int i = 0; i < headerNames.length; i++) {
            Cell headerCell = headerRow.createCell(i);
            headerCell.setCellValue(headerNames[i]);
        }

        int rowIndex = 1;

        List<ProductsBrand> products = productsBrandRepository.findByBrand(users.getBrand()); // 내가 팔고있는 상품 찾기
        log.info(products);
        for(ProductsBrand productsBrand : products) {
            List<ProductsOption> productsOptions = productsOptionRepository.findByProductId(productsBrand.getProducts().getProductId()); // 상품의 옵션 찾기
            log.info(productsOptions);
            for(ProductsOption productsOption : productsOptions) {
                List<Orders> ordersList = ordersRepository.findByStatusAndProductsOption("결제 완료",productsOption); // 결제 완료인 주문 찾기
                log.info(ordersList);
                for(Orders order : ordersList) {
                    Row row = sheet.createRow(rowIndex++);
                    row.createCell(0).setCellValue(order.getOrderId());
                    row.createCell(1).setCellValue(order.getCreateAt().toString());
                    row.createCell(2).setCellValue(order.getAmount());
                    row.createCell(3).setCellValue(order.getQuantity());
                    row.createCell(4).setCellValue(order.getReq());
                    row.createCell(5).setCellValue(order.getStatus());
                    row.createCell(6).setCellValue(order.getProductsOption().getProducts().getProductNumber());
                    row.createCell(7).setCellValue(order.getProductsOption().getProducts().getName());
                    row.createCell(8).setCellValue(order.getProductsOption().getProducts().getColor());
                    row.createCell(9).setCellValue(order.getProductsOption().getSize());
                    row.createCell(10).setCellValue(order.getCustomer().getName());
                    row.createCell(11).setCellValue("");
                    row.createCell(12).setCellValue("");
                }
            }
        }

        // Write to ByteArrayOutputStream
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            workbook.close();
            return outputStream.toByteArray();
        }
    }

    // 운송장 등록하기
    @Transactional
    public void parseOrderExcelFile(MultipartFile excel) throws IOException {
        if (excel.isEmpty()) {
            throw new RuntimeException("업로드된 파일이 비어 있습니다.");
        }

        try (InputStream fis = excel.getInputStream(); Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                // 첫 번째 행(헤더)은 건너뜀
                if (row.getRowNum() == 0 || row.getCell(0) == null) {continue;}


                log.info("row"+row.getCell(0));
                
                Cell cell11 = row.getCell(11);
                Cell cell12 = row.getCell(12);
                String trackingNumber = "";
                if (cell12.getCellType() == CellType.NUMERIC) {
                    double numericValue = cell12.getNumericCellValue();
                    trackingNumber = String.valueOf((long)numericValue);
                } else if (cell12.getCellType() == STRING) {
                    String stringValue = cell12.getStringCellValue();
                    trackingNumber = stringValue;
                } else {
                    // 다른 셀 타입 처리
                }


                if (cell11 == null || cell12 == null) {
                    throw new RuntimeException("필수 셀 값이 비어 있습니다. 행 번호: " + row.getRowNum());
                }

                String carrierId = cell11.getStringCellValue();


                String url = "https://apis.tracker.delivery/graphql"; // api 요청 주소

                // HTTP 헤더 설정
                HttpHeaders headers = new HttpHeaders();
                headers.set("Content-Type", "application/json");
                headers.set("Authorization", "TRACKQL-API-KEY 97vetlqkkgltulpe7tmfq9pr2:3ii271qrmmir9pvnsj23s34cvv2l3nokhhn9desbbtkb08cqjoi");

                // GraphQL 요청 내용
                String query = "query Track($carrierId: ID!, $trackingNumber: String!) {" +
                        "track(carrierId: $carrierId, trackingNumber: $trackingNumber) {" +
                        "lastEvent {" +
                        "time " +
                        "status {" +
                        "code " +
                        "}" +
                        "}" +
                        "}" +
                        "}";

                // 요청 바디 작성
                Map<String, Object> variables = new HashMap<>();
                variables.put("carrierId", carrierId);
                variables.put("trackingNumber", trackingNumber); // 문자열로 변환된 trackingNumber

                Map<String, Object> body = new HashMap<>();
                body.put("query", query);
                body.put("variables", variables);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

                try {
                    // API 요청
                    ResponseEntity<String> response = restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            entity,
                            String.class
                    );

                    String responseBody = response.getBody();
                    if (responseBody != null) {
                        JsonNode jsonNode = jacksonObjectMapper.readTree(responseBody);
                        JsonNode errorsNode = jsonNode.path("errors");
                        JsonNode dataNode = jsonNode.path("data").path("track");

                        // 처리 성공 여부 확인
                        if (errorsNode.isArray() && errorsNode.size() > 0) {
                            JsonNode firstError = errorsNode.get(0);
                            String errorMessage = firstError.path("message").asText();
                            log.error("Error message: " + errorMessage); // 에러 메시지 띄우기
                        } else {
                            // 해당 행에 해당하는 주문 찾기
                            Orders order = ordersRepository.findByOrderId((long)row.getCell(0).getNumericCellValue()).orElse(null);

                            log.info(order);

                            // 주문이 배송사에 등록됐는지 여부 확인
                            DeliveryTrack deliveryTrack = deliveryTrackRepository.findByOrders(order);
                            if(deliveryTrack != null) {
                                throw new RemoteException("이미 존재하는 배송 정보입니다.");
                            } else {
                                DeliveryTrack newDeliveryTrack = DeliveryTrack.builder()
                                        .carrierId(carrierId)
                                        .trackingNumber(trackingNumber)
                                        .order(order)
                                        .build();
                                deliveryTrackRepository.save(newDeliveryTrack);
                            }
                            order.setStatus("출고 완료");
                            ordersRepository.save(order);

                        }
                    } else {
                        log.warn("Response body is null.");
                    }
                } catch (HttpClientErrorException | HttpServerErrorException e) {
                    log.error("HTTP error during API request: " + e.getMessage(), e);
                } catch (Exception e) {
                    log.error("Unexpected error: " + e.getMessage(), e);
                }
            }
        }
    }


    // 기간 별 판매내역 엑셀 다운로드
    public byte[] generateExcelForPeriod(LocalDate startDate, LocalDate endDate) throws IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserName = authentication.getName();
        Optional<Users> optionalUser = userRepository.findByEmail(currentUserName);

        if (optionalUser.isEmpty()) {
            throw new RuntimeException("User not found: " + currentUserName);
        }

        Users users = optionalUser.get();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");
        sheet.setDefaultColumnWidth(10);

        // Header data
        int rowCount = 0;
        String headerNames[] = new String[]{
                "주문 번호",
                "결제 시간",
                "결제 금액",
                "수량",
                "요청 사항",
                "주문 상태",
                "품번",
                "상품 이름",
                "색상",
                "사이즈",
                "구매자",
        };

        Row headerRow = sheet.createRow(rowCount++);
        for (int i = 0; i < headerNames.length; i++) {
            Cell headerCell = headerRow.createCell(i);
            headerCell.setCellValue(headerNames[i]);
        }

        int rowIndex = 1;

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        // 내가 팔았던 주문 기간별 조회
        List<Orders> ordersList = ordersRepository.findByBrandAndDateRange(users.getBrand(),startDateTime,endDateTime);
        log.info(ordersList);
        for(Orders order : ordersList) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(order.getOrderNumber());
            row.createCell(1).setCellValue(order.getCreateAt().toString());
            row.createCell(2).setCellValue(order.getAmount());
            row.createCell(3).setCellValue(order.getQuantity());
            row.createCell(4).setCellValue(order.getReq());
            row.createCell(5).setCellValue(order.getStatus());
            row.createCell(6).setCellValue(order.getProductsOption().getProducts().getProductNumber());
            row.createCell(7).setCellValue(order.getProductsOption().getProducts().getName());
            row.createCell(8).setCellValue(order.getProductsOption().getProducts().getColor());
            row.createCell(9).setCellValue(order.getProductsOption().getSize());
            row.createCell(10).setCellValue(order.getCustomer().getName());
        }
        Row totalRow = sheet.createRow(rowIndex++);

// 매출 합계 레이블을 C열에 설정
        Cell totalLabelCell = totalRow.createCell(1);
        totalLabelCell.setCellValue("매출 합계");

// 매출 합계 수식을 D열에 설정 (C열의 합계를 계산)
        Cell totalCell = totalRow.createCell(2); // 합계를 표시할 셀을 D열에 설정
        String formula = String.format("SUM(C2:C%d)", rowIndex - 1); // 데이터 범위에 맞게 조정
        totalCell.setCellFormula(formula);


        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            workbook.close();
            return outputStream.toByteArray();
        }
    }


    // 환불 요청 엑셀 다운로드
    public byte[] generateCancelOrderExcel() throws IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserName = authentication.getName();
        Optional<Users> optionalUser = userRepository.findByEmail(currentUserName);

        if (optionalUser.isEmpty()) {
            throw new RuntimeException("User not found: " + currentUserName);
        }

        Users users = optionalUser.get();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");
        sheet.setDefaultColumnWidth(10);

        // Header data
        int rowCount = 0;
        String headerNames[] = new String[]{
                "주문 코드",
                "결제 시간",
                "결제 금액",
                "수량",
                "요청 사항",
                "주문 상태",
                "품번",
                "상품 이름",
                "색상",
                "사이즈",
                "구매자",
                "환불 요청 시간",
                "환불 사유",
                "상품 확인 여부"
        };

        Row headerRow = sheet.createRow(rowCount++);
        for (int i = 0; i < headerNames.length; i++) {
            Cell headerCell = headerRow.createCell(i);
            headerCell.setCellValue(headerNames[i]);
        }

        int rowIndex = 1;

        // 내 상품을 산 구매자가 환불 요청 한 주문 불러오기
        List<Orders> ordersList = ordersRepository.findByStatusAndBrand("환불 요청",users.getBrand());
        log.info(ordersList);
        for(Orders order : ordersList) {
            Row row = sheet.createRow(rowIndex++);
            OrdersRefund ordersRefund = ordersRefundRepository.findByOrderId(order.getOrderId());
            row.createCell(0).setCellValue(order.getOrderId());
            row.createCell(1).setCellValue(order.getCreateAt().toString());
            row.createCell(2).setCellValue(order.getAmount());
            row.createCell(3).setCellValue(order.getQuantity());
            row.createCell(4).setCellValue(order.getReq());
            row.createCell(5).setCellValue(order.getStatus());
            row.createCell(6).setCellValue(order.getProductsOption().getProducts().getProductNumber());
            row.createCell(7).setCellValue(order.getProductsOption().getProducts().getName());
            row.createCell(8).setCellValue(order.getProductsOption().getProducts().getColor());
            row.createCell(9).setCellValue(order.getProductsOption().getSize());
            row.createCell(10).setCellValue(order.getCustomer().getName());
            row.createCell(11).setCellValue(order.getUpdateAt().toString());
            row.createCell(12).setCellValue(ordersRefund.getReason());
            row.createCell(13).setCellValue("");
        }
        Row totalRow = sheet.createRow(rowIndex++);

        // Write to ByteArrayOutputStream
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            workbook.close();
            return outputStream.toByteArray();
        }
    }

    // 환불 요청 엑셀 업로드
    @Transactional
    public void parseCancelOrderExcelFile(MultipartFile excel) throws IOException {
        if (excel.isEmpty()) {
            throw new RuntimeException("업로드된 파일이 비어 있습니다.");
        }

        try (InputStream fis = excel.getInputStream(); Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);

            if (sheet == null) {
                throw new RuntimeException("엑셀 시트가 존재하지 않습니다.");
            }

            // 헤더 행 검사
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new RuntimeException("헤더 행이 존재하지 않습니다.");
            }


            for (Row row : sheet) {
                // 첫 번째 행(헤더)은 건너뜀
                if (row.getRowNum() == 0) continue;
                if(row.getCell(13) == null || row.getCell(13).getStringCellValue().trim().isEmpty() ||
                        !row.getCell(13).getStringCellValue().equals("확인")) continue; // 상품 확인 여부에 확인이라 적지 않으면 건너뛰기

                // 비어 있는 행 건너뜀
                if (row.getCell(0) == null || row.getCell(0).getCellType() == STRING) {
                    throw new RuntimeException("Product option ID not found in row " + row.getRowNum());
                }

                Cell cell = row.getCell(0);
                long orderId = 0;

                if (cell.getCellType() == CellType.NUMERIC) {
                    double numericValue = cell.getNumericCellValue();
                        orderId = (long)numericValue;
                } else if (cell.getCellType() == STRING) {
                    String stringValue = cell.getStringCellValue();
                    orderId = Long.parseLong(stringValue);

                }

                String reason = row.getCell(12).getStringCellValue();

                OrderCancelDTO dto = new OrderCancelDTO();
                dto.setOrderId(orderId);
                dto.setReason(reason);

                ordersService.cancelOrder(dto); // 결제 취소 메서드

            }
        }
    }


}


