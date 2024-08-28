package com.daewon.xeno_backend.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.daewon.xeno_backend.domain.*;
import com.daewon.xeno_backend.domain.auth.Users;
import com.daewon.xeno_backend.dto.UploadImageReadDTO;
import com.daewon.xeno_backend.dto.page.PageInfinityResponseDTO;
import com.daewon.xeno_backend.dto.page.PageRequestDTO;
import com.daewon.xeno_backend.dto.page.PageResponseDTO;
import com.daewon.xeno_backend.dto.product.*;
import com.daewon.xeno_backend.repository.*;
import com.daewon.xeno_backend.repository.Products.*;

import com.daewon.xeno_backend.repository.auth.UserRepository;
import com.daewon.xeno_backend.utils.CategoryUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductsRepository productsRepository;
    private final ProductsImageRepository productsImageRepository;
    private final ProductsStarRepository productsStarRepository;
    private final ProductsLikeRepository productsLikeRepository;
    private final ReviewRepository reviewRepository;
    private final ModelMapper modelMapper;
    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final ProductsSellerRepository productsSellerRepository;
    private final CartRepository cartRepository;
    private final ProductsSearchRepository productsSearchRepository;
    private final ProductsOptionRepository productsOptionRepository;
    private final ExcelService excelService;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    private final AmazonS3 s3Client;


    public String saveImage(MultipartFile image) {
        String fileName = image.getOriginalFilename();
        String uuid = UUID.randomUUID().toString();
        String key = uuid + "_" + fileName;

        try (InputStream inputStream = image.getInputStream()) {
            // S3에 파일 업로드
            s3Client.putObject(new PutObjectRequest(bucketName, key, inputStream, null));
            log.info("이미지 업로드 성공: " + key);

            // S3 URL 생성
            String fileUrl = "https://" + bucketName + ".s3.ap-northeast-2.amazonaws.com/" + key;
            return fileUrl; // S3에서의 객체 URL 반환
        } catch (IOException e) {
            log.error("파일 업로드 도중 오류가 발생했습니다: ", e);
            throw new RuntimeException("File upload error", e);
        }
    }

    @Override
    public void uploadImages(String productNumber, List<MultipartFile> productImages, MultipartFile productDetailImage)  {
        // Initialize URL fields
        String[] urls = new String[6]; // For up to 6 images

        // Ensure there are no more than 6 images
        int numberOfImages = Math.min(productImages.size(), urls.length);

        // Process each image
        for (int i = 0; i < numberOfImages; i++) {
            MultipartFile productImage = productImages.get(i);
            urls[i] = saveImage(productImage);
        }

        // Process detail image
        String detailUrl = saveImage(productDetailImage);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserName = authentication.getName();
        Users users = userRepository.findByEmail(currentUserName).orElse(null);

        // Create UploadImage object with the URLs
        ProductsImage productsImage = ProductsImage.builder()
                .productNumber(productNumber)
                .url_1(numberOfImages > 0 ? urls[0] : null)
                .url_2(numberOfImages > 1 ? urls[1] : null)
                .url_3(numberOfImages > 2 ? urls[2] : null)
                .url_4(numberOfImages > 3 ? urls[3] : null)
                .url_5(numberOfImages > 4 ? urls[4] : null)
                .url_6(numberOfImages > 5 ? urls[5] : null)
                .detail_url(detailUrl)
                .users(users)

                .build();

        productsImageRepository.save(productsImage);
    }

    @Transactional
//    @Scheduled(cron = "0 */2 * * * ?") // 매 2분마다 실행
    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정에 실행
    public void deleteOldS3Objects() {
        List<ProductsImage> oldImages = productsImageRepository.findImagesWithoutProductId();
        for (ProductsImage image : oldImages) {
            deleteObjectFromS3(image.getUrl_1());
            deleteObjectFromS3(image.getUrl_2());
            deleteObjectFromS3(image.getUrl_4());
            deleteObjectFromS3(image.getUrl_5());
            deleteObjectFromS3(image.getUrl_6());
            deleteObjectFromS3(image.getDetail_url());
            productsImageRepository.deleteById(image.getProductImageId());
        }
    }


    private void deleteObjectFromS3(String url) {
        if (url != null && !url.isEmpty()) {
            String key = extractKeyFromUrl(url); // URL에서 키 추출
            s3Client.deleteObject(bucketName, key);
        }
    }

    private String extractKeyFromUrl(String url) {
        // URL에서 S3 객체의 키를 추출하는 로직
        String keyPrefix = "https://" + bucketName + ".s3.ap-northeast-2.amazonaws.com/";
        if (url.startsWith(keyPrefix)) {
            return url.substring(keyPrefix.length());
        } else {
            throw new IllegalArgumentException("URL does not start with expected prefix: " + url);
        }
    }


    public static boolean equalsIgnoreNullAndEmpty(String a, String b) {
        return (a == null || a.isEmpty()) ? (b == null || b.isEmpty()) : a.equals(b);
    }


    @Transactional
    public void saveProductsFromExcel(MultipartFile excel) {
        try {

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserName = authentication.getName();
            Users users = userRepository.findByEmail(currentUserName).orElse(null);

            List<ProductRegisterDTO> productList = excelService.parseExcelFile(excel);
            // 엑셀에서 가져온 품번을 추출
            Set<String> productNumbersFromExcel = productList.stream()
                    .map(ProductRegisterDTO::getProductNumber)
                    .collect(Collectors.toSet());

            List<ProductsSeller> allProducts = productsSellerRepository.findByUsers(users);

            // 엑셀에 없는 품번을 데이터베이스에서 삭제
            for (ProductsSeller product : allProducts) {
                if (!productNumbersFromExcel.contains(product.getProducts().getProductNumber())) {
                    ProductsImage image = productsImageRepository.findByProductId(product.getProducts().getProductId());
                    deleteObjectFromS3(image.getUrl_1());
                    if (image.getUrl_2() != null) {
                        deleteObjectFromS3(image.getUrl_2());
                    }
                    if (image.getUrl_3() != null) {
                        deleteObjectFromS3(image.getUrl_3());
                    }
                    if (image.getUrl_4() != null) {
                        deleteObjectFromS3(image.getUrl_4());
                    }
                    if (image.getUrl_5() != null) {
                        deleteObjectFromS3(image.getUrl_5());
                    }
                    if (image.getUrl_6() != null) {
                        deleteObjectFromS3(image.getUrl_6());
                    }
                        deleteObjectFromS3(image.getDetail_url());
                    productsRepository.delete(product.getProducts());
                }
            }

            for (ProductRegisterDTO dto : productList) {
                Products existingProduct = productsRepository.findByProductNumber(dto.getProductNumber());
                if (existingProduct == null) {
                    Products newProduct = Products.builder()
                            .name(dto.getName())
                            .brandName("ASD")
                            .category(dto.getCategory())
                            .categorySub(dto.getCategorySub())
                            .price(dto.getPrice())
                            .priceSale(dto.getPriceSale())
                            .isSale(dto.isSale())
                            .season(dto.getSeason())
                            .productNumber(dto.getProductNumber())
                            .color(dto.getColors())
                            .build();
                    productsRepository.save(newProduct);
                    ProductsSeller productsSeller = ProductsSeller.builder()
                            .products(newProduct)
                            .users(users)
                            .build();
                    productsSellerRepository.save(productsSeller);

                    for(ProductSizeDTO size: dto.getSize()){
                        ProductsOption productsOption = ProductsOption.builder()
                                .products(newProduct)
                                .size(Size.valueOf(size.getSize()))
                                .stock(size.getStock())
                                .build();
                        productsOptionRepository.save(productsOption);
                    }

                    ProductsImage image = productsImageRepository.findByProductNumberAndUsers(dto.getProductNumber(),users);

                    // URL 일치 여부 확인
                    if (image != null) {
                        // 비교할 URL을 문자열로 저장
                        StringBuilder errorMessage = new StringBuilder("URLs do not match. Issues with: ");

                        boolean isValid = true;

                        // 각 URL 비교 및 일치하지 않는 경우 메시지 추가
                        if (!equalsIgnoreNullAndEmpty(image.getUrl_1(), dto.getUrl_1())) {
                            errorMessage.append("url_1, ");
                            isValid = false;
                        }
                        if (!equalsIgnoreNullAndEmpty(image.getUrl_2(), dto.getUrl_2())) {
                            errorMessage.append("url_2, ");
                            isValid = false;
                        }
                        if (!equalsIgnoreNullAndEmpty(image.getUrl_3(), dto.getUrl_3())) {
                            errorMessage.append("url_3, ");
                            isValid = false;
                        }
                        if (!equalsIgnoreNullAndEmpty(image.getUrl_4(), dto.getUrl_4())) {
                            errorMessage.append("url_4, ");
                            isValid = false;
                        }
                        if (!equalsIgnoreNullAndEmpty(image.getUrl_5(), dto.getUrl_5())) {
                            errorMessage.append("url_5, ");
                            isValid = false;
                        }
                        if (!equalsIgnoreNullAndEmpty(image.getUrl_6(), dto.getUrl_6())) {
                            errorMessage.append("url_6, ");
                            isValid = false;
                        }
                        if (!equalsIgnoreNullAndEmpty(image.getDetail_url(), dto.getDetail_url())) {
                            errorMessage.append("detail_url, ");
                            isValid = false;
                        }

                        // 오류 메시지에서 마지막 쉼표와 공백 제거
                        if (!isValid) {
                            errorMessage.setLength(errorMessage.length() - 2);
                            throw new IllegalStateException(errorMessage.toString());
                        }

                        image.setProducts(newProduct);
                        productsImageRepository.save(image);

                    } else {
                        throw new IllegalStateException("UploadImage not found. Operation cancelled.");
                    }
                } else {
                    existingProduct.setName(dto.getName());
                    existingProduct.setCategory(dto.getCategory());
                    existingProduct.setCategorySub(dto.getCategorySub());
                    existingProduct.setPrice(dto.getPrice());
                    existingProduct.setPriceSale(dto.getPriceSale());
                    existingProduct.setIsSale(dto.isSale());
                    existingProduct.setSeason(dto.getSeason());
                    existingProduct.setColor(dto.getColors());

                    productsRepository.save(existingProduct);
                    List<ProductsOption> productsColorSizes = productsOptionRepository.findByProductId(existingProduct.getProductId());
                    // 엑셀에서 가져온 사이즈 목록
                    Set<Size> sizesFromExcel = dto.getSize().stream()
                            .map(sizeDTO -> Size.valueOf(sizeDTO.getSize()))
                            .collect(Collectors.toSet());

                    // 기존 사이즈 목록을 사이즈 이름으로 매핑
                    Map<Size, ProductsOption> existingColorSizeMap = productsColorSizes.stream()
                            .collect(Collectors.toMap(ProductsOption::getSize, colorSize -> colorSize));

                    // 엑셀에서 가져온 사이즈를 기반으로 업데이트 및 추가 작업
                    for (ProductSizeDTO sizeDTO : dto.getSize()) {
                        Size size = Size.valueOf(sizeDTO.getSize());
                        ProductsOption existingColorSize = existingColorSizeMap.get(size);

                        if (existingColorSize == null) {
                            // 기존에 사이즈가 없는 경우 새로 생성
                            existingColorSize = ProductsOption.builder()
                                    .products(existingProduct)
                                    .size(size)
                                    .stock(sizeDTO.getStock()) // 여기서 사이즈와 함께 초기 재고도 설정
                                    .build();
                            productsOptionRepository.save(existingColorSize);
                        } else {
                            // 기존 사이즈가 있는 경우 재고 업데이트
                            existingColorSize.setStock(sizeDTO.getStock());
                            productsOptionRepository.save(existingColorSize);
                        }
                    }

                    // 엑셀 데이터에 없는 사이즈 삭제
                    for (ProductsOption colorSize : productsColorSizes) {
                        if (!sizesFromExcel.contains(colorSize.getSize())) {
                            productsOptionRepository.delete(colorSize);
                        }
                    }

                    ProductsImage image = productsImageRepository.findByProductNumberAndUsers(dto.getProductNumber(),users);

                    // URL 일치 여부 확인
                    if (image != null) {
                        // 비교할 URL을 문자열로 저장
                        StringBuilder errorMessage = new StringBuilder("URLs do not match. Issues with: ");

                        boolean isValid = true;

                        // 각 URL 비교 및 일치하지 않는 경우 메시지 추가
                        if (!equalsIgnoreNullAndEmpty(image.getUrl_1(), dto.getUrl_1())) {
                            errorMessage.append("url_1, ");
                            isValid = false;
                        }
                        if (!equalsIgnoreNullAndEmpty(image.getUrl_2(), dto.getUrl_2())) {
                            errorMessage.append("url_2, ");
                            isValid = false;
                        }
                        if (!equalsIgnoreNullAndEmpty(image.getUrl_3(), dto.getUrl_3())) {
                            errorMessage.append("url_3, ");
                            isValid = false;
                        }
                        if (!equalsIgnoreNullAndEmpty(image.getUrl_4(), dto.getUrl_4())) {
                            errorMessage.append("url_4, ");
                            isValid = false;
                        }
                        if (!equalsIgnoreNullAndEmpty(image.getUrl_5(), dto.getUrl_5())) {
                            errorMessage.append("url_5, ");
                            isValid = false;
                        }
                        if (!equalsIgnoreNullAndEmpty(image.getUrl_6(), dto.getUrl_6())) {
                            errorMessage.append("url_6, ");
                            isValid = false;
                        }
                        if (!equalsIgnoreNullAndEmpty(image.getDetail_url(), dto.getDetail_url())) {
                            errorMessage.append("detail_url ");
                            isValid = false;
                        }

                        // 오류 메시지에서 마지막 쉼표와 공백 제거
                        if (!isValid) {
                            errorMessage.setLength(errorMessage.length() - 2);
                            throw new IllegalStateException(errorMessage.toString());
                        }
                    } else {
                        throw new IllegalStateException("UploadImage not found. Operation cancelled.");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Handle the exception
        }
    }

    @Override
    public PageResponseDTO<ProductsSearchDTO> BrandNameOrNameOrCategoryOrCategorysubSearch(String keyword, PageRequestDTO pageRequestDTO) {
        // 페이지 요청 객체 생성
        Pageable pageable = PageRequest.of(
                pageRequestDTO.getPageIndex() <= 0 ? 0 : pageRequestDTO.getPageIndex() - 1,
                pageRequestDTO.getSize());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserName = authentication.getName();
        Users users = userRepository.findByEmail(currentUserName).orElse(null);

        // 카테고리를 기준으로 상품을 검색
        Page<Long> result = productsSearchRepository.findProductIdsByKeyword(keyword, pageable);
        log.info(result);

        // 검색 결과를 DTO로 변환
        List<ProductsSearchDTO> productList = result.getContent().stream().map(productId -> {
                    Products product = productsRepository.findById(productId)
                            .orElseThrow(() -> new EntityNotFoundException("ProductColor not found"));

                    ProductsSearchDTO dto = ProductsSearchDTO.builder()
                            .productId(productId)
                            .brandName(product.getBrandName())
                            .name(product.getName())
                            .category(product.getCategory())
                            .categorySub(product.getCategorySub())
                            .price(product.getPrice())
                            .priceSale(product.getPriceSale())
                            .isSale(product.getIsSale())
                            .build();

                    if (users != null) {
                        LikeProducts likeProducts = likeRepository.findByProductIdAndUserId(productId, users.getUserId());
                        dto.setLike(likeProducts != null ? likeProducts.isLike() : false);
                    }else {
                        dto.setLike(false);
                    }

                    log.info(dto.isLike());

                    return dto;
                })
                .collect(Collectors.toList());

        // 페이지 응답 객체 생성 및 반환
        return PageResponseDTO.<ProductsSearchDTO>withAll()
                .pageRequestDTO(pageRequestDTO)
                .dtoList(productList)
                .totalIndex((int) result.getTotalElements())
                .build();
    }


    @Override
    public ProductColorInfoCardDTO getProductCardInfo(Long productId) {
        Products products = productsRepository.findById(productId).orElse(null);
        ProductsLike productsLike = productsLikeRepository.findByProductId(productId).orElse(null);
        ProductsStar productsStar = productsStarRepository.findByProductId(productId).orElse(null);
        ProductColorInfoCardDTO dto = ProductColorInfoCardDTO.builder()
                .productId(productId)
                .name(products.getName())
                .color(products.getColor())
                .brandName(products.getBrandName())
                .category(products.getCategory())
                .categorySub(products.getCategorySub())
                .isSale(products.getIsSale())
                .price(products.getPrice())
                .priceSale(products.getPriceSale())
                .starAvg(productsStar != null ? productsStar.getStarAvg() : 0)
                .likeIndex(productsLike != null ? productsLike.getLikeIndex() : 0)
                .build();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String currentUserName = authentication.getName();

        Users users = userRepository.findByEmail(currentUserName)
                .orElse(null);

        if (users != null) {
            LikeProducts likeProducts = likeRepository.findByProductIdAndUserId(productId, users.getUserId());
            if (likeProducts != null) {
                dto.setLike(likeProducts.isLike());
            } else {
                dto.setLike(false);
            }
        } else {
            dto.setLike(false);
        }


        return dto;
    }

    @Override
    public ProductDetailImagesDTO getProductDetailImages(Long productId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductsImage> productDetailImages = productsImageRepository
                .findByProductIdPaging(productId, pageable);
        long count = productDetailImages.getTotalElements();


        ProductDetailImagesDTO productDetailImagesDTO = new ProductDetailImagesDTO();
        productDetailImagesDTO.setProductImages(null);
        productDetailImagesDTO.setImagesCount(count);
        log.info("카운트" + count);
        log.info("카운트1" + productDetailImages);
        return productDetailImagesDTO;
    }




    @Override
    public ProductOrderBarDTO getProductOrderBar(Long productId) {
        ProductOrderBarDTO dto = new ProductOrderBarDTO();
        log.info("Initial DTO: " + dto);
        List<ProductStockDTO> productsStockDTO = new ArrayList<>();

        try {
            // 상품 좋아요 수 가져오기
            ProductsLike productsLike = productsLikeRepository.findByProductId(productId).orElse(null);
            dto.setLikeIndex(productsLike != null ? productsLike.getLikeIndex() : 0);

            Products products = productsRepository.findById(productId).orElse(null);

            dto.setPrice(products.getIsSale() ? products.getPriceSale() : products.getPrice());

                List<ProductsOption> productsOptions = productsOptionRepository.findByProductId(products.getProductId());
                for (ProductsOption pcs : productsOptions) {
                    ProductStockDTO stockDTO = new ProductStockDTO();
                    stockDTO.setProductId(pcs.getProducts().getProductId());
                    stockDTO.setProductOptionId(pcs.getProductOptionId());
                    stockDTO.setColor(pcs.getProducts().getColor());
                    stockDTO.setSize(pcs != null ? pcs.getSize().name() : "에러");
                    stockDTO.setStock(pcs.getStock());
                    productsStockDTO.add(stockDTO);
                }
            dto.setOrderInfo(productsStockDTO);

            // 상품 사이즈 및 재고 정보 가져오기

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserName = authentication.getName();
            log.info("이름:" + currentUserName);
            Users users = userRepository.findByEmail(currentUserName)
                    .orElse(null); // 유저 객체 생성

            if (users != null) { // 로그인한 경우
                Long userId = users.getUserId();
                LikeProducts likeProducts = likeRepository.findByProductIdAndUserId(productId, userId);
                dto.setLike(likeProducts != null ? likeProducts.isLike() : false); // 즐겨찾기 여부
            } else {
                dto.setLike(false); // 로그인 안한경우 무조건 false
            }

        } catch (DataAccessException e) {
            log.error("Data access error while fetching product order bar details: " + e.getMessage(), e);
        }

        return dto;
    }

    @Override
    public List<ProductColorInfoCardDTO> getProductsInfoByCategory(String categoryId, String categorySubId) {
        List<Products> productsList = new ArrayList<>();
        if (categoryId.equals("000") && categorySubId.isEmpty()) {
            productsList = productsRepository.findAll();
        } else if (categorySubId.isEmpty()) {
            String category = CategoryUtils.getCategoryFromCode(categoryId);
            productsList = productsRepository.findByCategory(category);
        } else {
            String category = CategoryUtils.getCategoryFromCode(categoryId);
            String categorySub = CategoryUtils.getCategorySubFromCode(categorySubId);
            productsList = productsRepository.findByCategorySub(category, categorySub);
        }

        List<ProductColorInfoCardDTO> productsInfoCardDTOList = new ArrayList<>();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String currentUserName = authentication.getName();

        String email = "joohyeongzz@naver.com";

        Users users = userRepository.findByEmail(currentUserName)
                .orElse(null);

        log.info(users);

        for (Products products : productsList) {
                ProductColorInfoCardDTO dto = new ProductColorInfoCardDTO();
                dto.setBrandName(products.getBrandName());
                dto.setName(products.getName());
                dto.setCategory(products.getCategory());
                dto.setCategorySub(products.getCategorySub());
                dto.setPrice(products.getPrice());
                dto.setPriceSale(products.getPriceSale());
                dto.setSale(products.getIsSale());
                dto.setColor(products.getColor());
                if (users != null) {
                    Long userId = users.getUserId();
                    LikeProducts likeProducts = likeRepository
                            .findByProductIdAndUserId(products.getProductId(), userId);
                    dto.setLike(likeProducts != null ? likeProducts.isLike() : false);
                } else {
                    dto.setLike(false);
                }
                ProductsLike productsLike = productsLikeRepository
                        .findByProductId(products.getProductId()).orElse(null);
                ProductsStar productsStar = productsStarRepository
                        .findByProductId(products.getProductId()).orElse(null);
                ProductsImage productsImage = productsImageRepository
                        .findByProductId(products.getProductId());


                dto.setProductImage(productsImage.getUrl_1());

                dto.setProductId(products.getProductId());
                dto.setLikeIndex(productsLike != null ? productsLike.getLikeIndex() : 0);
                dto.setStarAvg(productsStar != null ? productsStar.getStarAvg() : 0);

                productsInfoCardDTOList.add(dto);
            }
        return productsInfoCardDTOList;
    }

    @Override
    public List<ProductColorInfoCardDTO> getLikedProductsInfo() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String currentUserName = authentication.getName();

        Users users = userRepository.findByEmail(currentUserName)
                .orElse(null);

        log.info(users);
        List<LikeProducts> likeProductsList = likeRepository.findByUserId(users.getUserId());
        log.info(likeProductsList);
        List<ProductColorInfoCardDTO> productsInfoCardDTOList = new ArrayList<>();

        for (LikeProducts likeProducts : likeProductsList) {
            ProductColorInfoCardDTO dto = new ProductColorInfoCardDTO();
            ProductsLike productsLike = productsLikeRepository
                    .findById(likeProducts.getProductsLike().getProductLikeId()).orElse(null);

            Products products = productsLike.getProducts();

            ProductsStar productsStar = productsStarRepository.findByProductId(products.getProductId())
                    .orElse(null);


            dto.setBrandName(products.getBrandName());

            dto.setName(products.getName());

            dto.setCategory(products.getCategory());

            dto.setCategorySub(products.getCategorySub());

            dto.setPrice(products.getPrice());

            dto.setPriceSale(products.getPriceSale());

            dto.setSale(products.getIsSale());
            dto.setColor(products.getColor());

            dto.setLike(likeProducts.isLike());
            dto.setProductId(products.getProductId());
            dto.setLikeIndex(productsLike != null ? productsLike.getLikeIndex() : 0);
            dto.setStarAvg(productsStar != null ? productsStar.getStarAvg() : 0);

            ProductsImage productsImage = productsImageRepository
                    .findByProductId(products.getProductId());


            dto.setProductImage(productsImage.getUrl_1());
            productsInfoCardDTOList.add(dto);
            log.info(productsInfoCardDTOList);
            log.info(dto);
        }

        return productsInfoCardDTOList;
    }

    @Override
    public List<ProductListBySellerDTO> getProductListBySeller(String email) {


        Users users = userRepository.findByEmail(email).orElse(null);

        List<ProductsSeller> list = productsSellerRepository.findByUsers(users);
        List<ProductListBySellerDTO> dtoList = new ArrayList<>();

        for(ProductsSeller productsSeller: list){
            ProductListBySellerDTO dto = new ProductListBySellerDTO();
            dto.setProductId(productsSeller.getProducts().getProductId());
            dto.setProductNumber(productsSeller.getProducts().getProductNumber());
            dto.setProductName(productsSeller.getProducts().getName());
            dtoList.add(dto);
        }
        return dtoList;
    }

    @Override
    public PageInfinityResponseDTO<ProductsStarRankListDTO> getrankTop50(String category, PageRequestDTO pageRequestDTO) {
        Pageable pageable = pageRequestDTO.getPageable();
        Page<ProductsStar> productsStarPage = productsStarRepository.findByStarAvgDesc(category, pageable);

        log.info(category);
        log.info(productsStarPage);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserName = authentication.getName();
        log.info(currentUserName);

        Users users = userRepository.findByEmail(currentUserName).orElse(null);

        log.info(users);

        List<ProductsStarRankListDTO> dtoList = productsStarPage.getContent().stream()
                .map(productsStar -> {
                    Products product = productsStar.getProducts();
                    Long productId = productsStar.getProducts().getProductId();

                    ProductsStarRankListDTO dto = ProductsStarRankListDTO.builder()
                            .productId(productId)
                            .brandName(product.getBrandName())
                            .price(product.getPrice())
                            .priceSale(product.getPriceSale())
                            .isSale(product.getIsSale())
                            .category(product.getCategory())
                            .categorySub(product.getCategorySub())
                            .name(product.getName())
                            .build();

                    if (users != null) {
                        LikeProducts likeProducts = likeRepository.findByProductIdAndUserId(productId, users.getUserId());
                        dto.setLike(likeProducts != null ? likeProducts.isLike() : false);
                    }else {
                        dto.setLike(false);
                    }

                    return dto;
                })
                .collect(Collectors.toList());

        log.info(dtoList);

        return PageInfinityResponseDTO.<ProductsStarRankListDTO>withAll()
                .pageRequestDTO(pageRequestDTO)
                .dtoList(dtoList)
                .totalIndex((int) productsStarPage.getTotalElements())
                .build();
    }

    // 카테고리 별 랭크 10개
    @Override
    public List<ProductsStarRankListDTO> getranktop10(String category) {
        List<ProductsStar> top10Products = productsStarRepository.findByTop10StarAvgDesc(category);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserName = authentication.getName();
        Users users = userRepository.findByEmail(currentUserName).orElse(null);

        log.info(top10Products);
        return top10Products.stream()
                .map(productsStar -> {

                    Products product = productsStar.getProducts();
                    Long productId = productsStar.getProducts().getProductId();

                    ProductsStarRankListDTO dto = ProductsStarRankListDTO.builder()
                            .productId(productId)
                            .brandName(product.getBrandName())
                            .name(product.getName())
                            .price(product.getPrice())
                            .priceSale(product.getPriceSale())
                            .isSale(product.getIsSale())
                            .category(product.getCategory())
                            .categorySub(product.getCategorySub())
                            .name(product.getName())
                            .build();

                    if (users != null) {
                        LikeProducts likeProducts = likeRepository.findByProductIdAndUserId(productId, users.getUserId());
                        dto.setLike(likeProducts != null ? likeProducts.isLike() : false);
                    }else {
                        dto.setLike(false);
                    }


                    return dto;
                })
                .limit(10)
                .collect(Collectors.toList());
    }


    @Override
    public ProductInfoDTO getProductInfo(Long productId) {

        Products products = productsRepository.findByProductId(productId);
        ProductInfoDTO productInfoDTO = modelMapper.map(products, ProductInfoDTO.class); // dto 매핑

        productInfoDTO.setProductId(products.getProductId());
        productInfoDTO.setBrandName(products.getBrandName());
        productInfoDTO.setName(products.getName());
        productInfoDTO.setCategory(products.getCategory());
        productInfoDTO.setCategorySub(products.getCategorySub());
        productInfoDTO.setPrice(products.getPrice());
        productInfoDTO.setPriceSale(products.getPriceSale());
        productInfoDTO.setProductNumber(products.getProductNumber());
        productInfoDTO.setSeason(products.getSeason());
        productInfoDTO.setSale(products.getIsSale());
        productInfoDTO.setColor(products.getColor());


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        log.info(authentication);
        String currentUserName = authentication.getName();

        log.info(currentUserName);


        Users users = userRepository.findByEmail(currentUserName)
                .orElse(null);

        if (users != null) {
            Long userId = users.getUserId();
            LikeProducts likeProducts = likeRepository.findByProductIdAndUserId(productId, userId);
            productInfoDTO.setLike(likeProducts != null ? likeProducts.isLike() : false);
        } else {
            productInfoDTO.setLike(false);
        }

        ProductsStar productsStar = productsStarRepository.findByProductId(productId).orElse(null);

        productInfoDTO.setStarAvg(productsStar != null ? productsStar.getStarAvg() : 0);

        ProductsLike productsLike = productsLikeRepository.findByProductId(productId).orElse(null);

        productInfoDTO.setLikeIndex(productsLike != null ? productsLike.getLikeIndex() : 0);

        productInfoDTO.setReviewIndex(
                reviewRepository.countByProductId(productId) != 0
                        ? reviewRepository.countByProductId(productId)
                        : 0);

        ProductsImage productImages = productsImageRepository.findByProductId(products.getProductId());
        productInfoDTO.setUrl_1(productImages.getUrl_1());
        productInfoDTO.setUrl_2(productImages.getUrl_2());
        productInfoDTO.setUrl_3(productImages.getUrl_3());
        productInfoDTO.setUrl_4(productImages.getUrl_4());
        productInfoDTO.setUrl_5(productImages.getUrl_5());
        productInfoDTO.setUrl_6(productImages.getUrl_6());
        productInfoDTO.setDetail_url(productImages.getDetail_url());

        return productInfoDTO;
    }

    @Override
    public List<UploadImageReadDTO> getUploadImageAll() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserName = authentication.getName();

        Optional<Users> optionalUser = userRepository.findByEmail(currentUserName);
        if (optionalUser.isEmpty()) {
            throw new RuntimeException("User not found: " + currentUserName);
        }

        Users users = optionalUser.get();
        List<ProductsImage> images = productsImageRepository.findByUsers(users);

        return images.stream()
                .map(this::convertToUploadImageReadDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UploadImageReadDTO getUploadImageByProductNumber(String productNumber) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserName = authentication.getName();
        Optional<Users> optionalUser = userRepository.findByEmail(currentUserName);
        if (optionalUser.isEmpty()) {
            throw new RuntimeException("User not found: " + currentUserName);
        }

        Users users = optionalUser.get();
        ProductsImage image = productsImageRepository.findByProductNumberAndUsers(productNumber, users);

        return convertToUploadImageReadDTO(image);
    }

    private UploadImageReadDTO convertToUploadImageReadDTO(ProductsImage image) {
        UploadImageReadDTO dto = new UploadImageReadDTO();
        dto.setProductNumber(image.getProductNumber());
        dto.setUrl_1(image.getUrl_1());
        dto.setUrl_2(image.getUrl_2());
        dto.setUrl_3(image.getUrl_3());
        dto.setUrl_4(image.getUrl_4());
        dto.setUrl_5(image.getUrl_5());
        dto.setUrl_6(image.getUrl_6());
        dto.setDetail_url_1(image.getDetail_url());
        return dto;
    }


    public byte[] generateExcelFile() throws IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserName = authentication.getName();
        Optional<Users> optionalUser = userRepository.findByEmail(currentUserName);
        if (optionalUser.isEmpty()) {
            throw new RuntimeException("User not found: " + currentUserName);
        }

        Users users = optionalUser.get();

        List<ProductsSeller> products = productsSellerRepository.findByUsers(users);

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
                "S 재고",
                "M 재고",
                "L 재고",
                "XL 재고",
                "이미지1",
                "이미지2",
                "이미지3",
                "이미지4",
                "이미지5",
                "이미지6",
                "상세이미지6",
                "시즌"
        };

        Row headerRow = sheet.createRow(rowCount++);
        for (int i = 0; i < headerNames.length; i++) {
            Cell headerCell = headerRow.createCell(i);
            headerCell.setCellValue(headerNames[i]);
        }

        // 카테고리와 서브 카테고리 데이터
        String[] categories = {"상의", "하의", "아우터"};

        // 카테고리 드롭다운 설정
        DataValidationHelper validationHelper = sheet.getDataValidationHelper();
        DataValidationConstraint categoryConstraint = validationHelper.createExplicitListConstraint(categories);
        CellRangeAddressList categoryAddressList = new CellRangeAddressList(1, 100, 2, 2); // C열에 적용
        DataValidation categoryValidation = validationHelper.createValidation(categoryConstraint, categoryAddressList);
        categoryValidation.setShowErrorBox(true);
        sheet.addValidationData(categoryValidation);

        String[] subCategoriesTops = {"반팔", "긴팔"};
        String[] subCategoriesBottoms = {"청바지", "면", "반바지", "나일론"};
        String[] subCategoriesOuterwear = {"코트", "후드집업", "바람막이",};



        int rowIndex = 1;
        for (ProductsSeller product : products) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(product.getProducts().getProductNumber());
            row.createCell(1).setCellValue(product.getProducts().getName());
            row.createCell(2).setCellValue(product.getProducts().getCategory());
            row.createCell(3).setCellValue(product.getProducts().getCategorySub());
            row.createCell(4).setCellValue(product.getProducts().getPrice());
            row.createCell(5).setCellValue(product.getProducts().getPriceSale() != 0 ? product.getProducts().getPriceSale() : 0);
            row.createCell(6).setCellValue(product.getProducts().getColor());
            row.createCell(7).setCellValue(0); // S size stock
            row.createCell(8).setCellValue(0); // M size stock
            row.createCell(9).setCellValue(0); // L size stock
            row.createCell(10).setCellValue(0); // XL size stock

            List<ProductsOption> productsOptions = productsOptionRepository.findByProductId(product.getProducts().getProductId());

            for (ProductsOption productsOption : productsOptions) {
                if (productsOption.getSize() == Size.S) {
                    row.createCell(7).setCellValue(productsOption.getStock());
                } else if (productsOption.getSize() == Size.M) {
                    row.createCell(8).setCellValue(productsOption.getStock());
                } else if (productsOption.getSize() == Size.L) {
                    row.createCell(9).setCellValue(productsOption.getStock());
                } else if (productsOption.getSize() == Size.XL) {
                    row.createCell(10).setCellValue(productsOption.getStock());
                }
            }

            ProductsImage productsImage = productsImageRepository.findByProductId(product.getProducts().getProductId());

            row.createCell(11).setCellValue(productsImage.getUrl_1() == null ? "" : productsImage.getUrl_1());
            row.createCell(12).setCellValue(productsImage.getUrl_2() == null ? "" : productsImage.getUrl_2());
            row.createCell(13).setCellValue(productsImage.getUrl_3() == null ? "" : productsImage.getUrl_3());
            row.createCell(14).setCellValue(productsImage.getUrl_4() == null ? "" : productsImage.getUrl_4());
            row.createCell(15).setCellValue(productsImage.getUrl_5() == null ? "" : productsImage.getUrl_5());
            row.createCell(16).setCellValue(productsImage.getUrl_6() == null ? "" : productsImage.getUrl_6());
            row.createCell(17).setCellValue(productsImage.getDetail_url() == null ? "" : productsImage.getDetail_url());
            row.createCell(18).setCellValue(product.getProducts().getSeason());

            }

        // Write to ByteArrayOutputStream
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            workbook.close();
            return outputStream.toByteArray();
        }
    }


}