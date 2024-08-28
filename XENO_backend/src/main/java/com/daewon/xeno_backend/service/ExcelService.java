package com.daewon.xeno_backend.service;


import com.daewon.xeno_backend.dto.product.ProductRegisterDTO;
import com.daewon.xeno_backend.dto.product.ProductSizeDTO;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@Log4j2
public class ExcelService {

    public List<ProductRegisterDTO> parseExcelFile(MultipartFile excel) throws IOException {
        List<ProductRegisterDTO> productList = new ArrayList<>();

        try (InputStream fis = excel.getInputStream(); Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0 || row.getCell(0) == null) continue; // Skip header row
                int rowIndex = 0; // 반복 횟수를 추적하기 위한 변수 선언
                ProductRegisterDTO product = new ProductRegisterDTO();
                log.info(rowIndex+"번쨰"+row.getCell(0));

                Cell cell = row.getCell(0); // Assuming column 0 for Product Number
                String productNumber = "";

                if (cell != null) {
                    switch (cell.getCellType()) {
                        case STRING:
                            productNumber = cell.getStringCellValue();
                            break;
                        case NUMERIC:
                            // Convert numeric value to string
                            productNumber = String.valueOf((int) cell.getNumericCellValue());
                            break;
                        default:
                            // Handle other types if necessary, or set a default value
                            productNumber = "";
                            break;
                    }
                }

                product.setProductNumber(productNumber);
                product.setName(row.getCell(1).getStringCellValue());
                product.setCategory(row.getCell(2).getStringCellValue());
                product.setCategorySub(row.getCell(3).getStringCellValue());
                product.setPrice((long)row.getCell(4).getNumericCellValue());
                product.setPriceSale(row.getCell(5) == null || row.getCell(5).getNumericCellValue() == 0 ? 0 : (long)row.getCell(5).getNumericCellValue());
                product.setSale(row.getCell(5) == null || row.getCell(5).getNumericCellValue() == 0 ? false : true);

                product.setColors(row.getCell(6).getStringCellValue());

                String sizeString = getCellValue(row.getCell(7)); // Comma-separated sizes
                String stockString = getCellValue(row.getCell(8)); // Comma-separated stocks

                String[] sizes = sizeString.split(",");
                String[] stocks = stockString.split(",");

                List<ProductSizeDTO> sizeDTOs = new ArrayList<>();
                for (int i = 0; i < sizes.length; i++) {
                    ProductSizeDTO sizeDTO = new ProductSizeDTO();
                    sizeDTO.setSize(sizes[i].trim());
                    sizeDTO.setStock(Integer.parseInt(stocks[i].trim()));
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
}


