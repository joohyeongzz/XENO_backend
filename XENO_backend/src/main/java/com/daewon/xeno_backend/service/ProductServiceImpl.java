package com.daewon.xeno_backend.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.daewon.xeno_backend.domain.*;
import com.daewon.xeno_backend.domain.auth.Users;
import com.daewon.xeno_backend.dto.page.PageInfinityResponseDTO;
import com.daewon.xeno_backend.dto.page.PageRequestDTO;
import com.daewon.xeno_backend.dto.page.PageResponseDTO;
import com.daewon.xeno_backend.dto.product.*;
import com.daewon.xeno_backend.repository.*;
import com.daewon.xeno_backend.security.exception.ProductNotFoundException;

import com.daewon.xeno_backend.utils.CategoryUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductsRepository productsRepository;
    private final ProductsImageRepository productsImageRepository;
    private final ProductsDetailImageRepository productsDetailImageRepository;
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
    private final UploadImageRepository uploadImageRepository;

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
            String fileUrl = "https://" + bucketName + ".s3.amazonaws.com/" + key;
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
        UploadImage uploadImage = UploadImage.builder()
                .productNumber(productNumber)
                .url_1(numberOfImages > 0 ? urls[0] : null)
                .url_2(numberOfImages > 1 ? urls[1] : null)
                .url_3(numberOfImages > 2 ? urls[2] : null)
                .url_4(numberOfImages > 3 ? urls[3] : null)
                .url_5(numberOfImages > 4 ? urls[4] : null)
                .url_6(numberOfImages > 5 ? urls[5] : null)
                .detail_url_1(detailUrl)
                .users(users)
                .build();

        uploadImageRepository.save(uploadImage);
    }

    public void saveProductsFromExcel(MultipartFile excel) {
        try {
            List<ProductRegisterDTO> productList = excelService.parseExcelFile(excel);
            // 엑셀에서 가져온 품번을 추출
            Set<String> productNumbersFromExcel = productList.stream()
                    .map(ProductRegisterDTO::getProductNumber)
                    .collect(Collectors.toSet());

            // 데이터베이스에서 모든 품번을 추출
            List<Products> allProducts = productsRepository.findAll();
            Set<String> productNumbersFromDatabase = allProducts.stream()
                    .map(Products::getProductNumber)
                    .collect(Collectors.toSet());

            // 엑셀에 없는 품번을 데이터베이스에서 삭제
            for (Products product : allProducts) {
                if (!productNumbersFromExcel.contains(product.getProductNumber())) {
                    productsRepository.delete(product);
                }
            }

            for (ProductRegisterDTO dto : productList) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String currentUserName = authentication.getName();
                Users users = userRepository.findByEmail(currentUserName).orElse(null);
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
                    ProductsImage productsImage = ProductsImage.builder()
                            .products(newProduct)
                            .url_1(dto.getUrl_1())
                            .url_2(dto.getUrl_2())
                            .url_3(dto.getUrl_3())
                            .url_4(dto.getUrl_4())
                            .url_5(dto.getUrl_5())
                            .url_6(dto.getUrl_6())
                            .build();
                    productsImageRepository.save(productsImage);


                    ProductsDetailImage productsDetailImage = ProductsDetailImage.builder()
                            .products(newProduct)
                            .url_1(dto.getDetail_url_1())
                            .build();
                    productsDetailImageRepository.save(productsDetailImage);

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
                    ProductsImage productsImage = productsImageRepository.findByProductId(existingProduct.getProductId());
                    productsImage.setUrl_1(dto.getUrl_1());
                    productsImage.setUrl_2(dto.getUrl_2());
                    productsImage.setUrl_3(dto.getUrl_3());
                    productsImage.setUrl_4(dto.getUrl_4());
                    productsImage.setUrl_5(dto.getUrl_5());
                    productsImage.setUrl_6(dto.getUrl_6());
                    productsImageRepository.save(productsImage);
                    ProductsDetailImage productsDetailImage = productsDetailImageRepository.findOneByProductId(existingProduct.getProductId());
                    productsDetailImage.setUrl_1(dto.getDetail_url_1());

                    productsDetailImageRepository.save(productsDetailImage);

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
        Page<ProductsDetailImage> productDetailImages = productsDetailImageRepository
                .findByProductId(productId, pageable);
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


                    dto.setProductImage(null);

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
        ProductsDetailImage productDetailImage = productsDetailImageRepository.findOneByProductId(products.getProductId());
        productInfoDTO.setDetail_url_1(productDetailImage.getUrl_1());


        return productInfoDTO;
    }



}