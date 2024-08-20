package com.daewon.xeno_backend.service;

import com.daewon.xeno_backend.domain.*;
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
    private final ProductsColorRepository productsColorRepository;
    private final ProductsColorSizeRepository productsColorSizeRepository;
    private final ProductsStockRepository productsStockRepository;
    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final ProductsSellerRepository productsSellerRepository;
    private final CartRepository cartRepository;
    private final ProductsSearchRepository productsSearchRepository;


    @Value("${uploadPath}")
    private String uploadPath;


    public byte[] getImage(String uuid, String fileName) throws IOException {
        String filePath = uploadPath + uuid + "_" + fileName;
        // 파일을 바이트 배열로 읽기
        Path path = Paths.get(filePath);
        byte[] image = Files.readAllBytes(path);
        return image;
    }

    @Override
    public Products createProduct(ProductRegisterDTO dto, List<MultipartFile> productImage, MultipartFile productDetailImage) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String currentUserName = authentication.getName();



        Users users = userRepository.findByEmail(currentUserName)
                .orElse(null);


        // 1. Products 엔티티 생성 및 저장
        Products product = Products.builder()
                .brandName(users.getBrandName())
                .name(dto.getName())
                .category(dto.getCategory())
                .categorySub(dto.getCategorySub())
                .price(dto.getPrice())
                .priceSale(dto.getPriceSale())
                .isSale(dto.isSale())
                .productNumber(dto.getProductNumber())
                .season(dto.getSeason())
                .build();
        productsRepository.save(product);





        ProductsSeller productsSeller = ProductsSeller.builder()
                .products(product)
                .users(users)
                .build();
        productsSellerRepository.save(productsSeller);

        ProductsColor productsColor = ProductsColor.builder()
                .products(product)
                .color(dto.getColors())
                .build();
        productsColorRepository.save(productsColor);


//             3. ProductsColorSize 엔티티 생성 및 저장
        for (ProductSizeDTO size : dto.getSize()) {
            ProductsColorSize productsColorSize = ProductsColorSize.builder()
                    .productsColor(productsColor)
                    .size(Size.valueOf(size.getSize()))
                    .build();
            productsColorSizeRepository.save(productsColorSize);

            // ProductsStock 엔티티 생성 및 저장
            ProductsStock productsStock = ProductsStock.builder()
                    .productsColorSize(productsColorSize)
                    .stock(size.getStock())  // 초기 재고를 100으로 설정
                    .build();
            productsStockRepository.save(productsStock);

        }

        if (productImage != null && !productImage.isEmpty()) {
            boolean isFirstImage = true;
            for (MultipartFile image : productImage) {
                String uuid = saveImage(image);

                boolean isMain = isFirstImage;
                isFirstImage = false; // 다음 이미지는 첫 번째 이미지가 아님
                ProductsImage productsImage = ProductsImage.builder()
                        .productsColor(productsColor)
                        .fileName(image.getOriginalFilename())
                        .uuid(uuid)
                        .isMain(isMain)
                        .build();
                productsImageRepository.save(productsImage);
            }
        }

        if (productDetailImage != null && !productDetailImage.isEmpty()) {
            String uuid = saveImage(productDetailImage);
            ProductsDetailImage productsDetailImage = ProductsDetailImage.builder()
                    .productsColor(productsColor)
                    .fileName(productDetailImage.getOriginalFilename())
                    .uuid(uuid)
                    .build();
            productsDetailImageRepository.save(productsDetailImage);
        }

        return product;
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
        Page<Long> result = productsSearchRepository.findProductColorIdsByKeyword(keyword, pageable);
        log.info(result);

        // 검색 결과를 DTO로 변환
        List<ProductsSearchDTO> productList = result.getContent().stream().map(productColorId -> {
                    ProductsColor productsColor = productsColorRepository.findById(productColorId)
                            .orElseThrow(() -> new EntityNotFoundException("ProductColor not found"));
                    Products product = productsColor.getProducts();


                    ProductsSearchDTO dto = ProductsSearchDTO.builder()
                            .productColorId(productColorId)
                            .brandName(product.getBrandName())
                            .name(product.getName())
                            .category(product.getCategory())
                            .categorySub(product.getCategorySub())
                            .price(product.getPrice())
                            .priceSale(product.getPriceSale())
                            .isSale(product.getIsSale())
                            .build();

                    if (users != null) {
                        LikeProducts likeProducts = likeRepository.findByProductColorIdAndUserId(productColorId, users.getUserId());
                        dto.setLike(likeProducts != null ? likeProducts.isLike() : false);
                    }else {
                        dto.setLike(false);
                    }

                    log.info(dto.isLike());

                    ProductsImage productImage = productsImageRepository.findByProductsColorProductColorIdAndIsMainTrue(productColorId);
                    if (productImage != null) {
                        try {
                            String filePath = uploadPath + productImage.getUuid() + "_" + productImage.getFileName();
                            Path path = Paths.get(filePath);
                            if (Files.exists(path)) {
                                byte[] imageData = Files.readAllBytes(path);
                                dto.setProductImage(imageData);
                            } else {
                                log.warn("이미지 파일이 존재하지 않습니다: " + filePath);
                            }
                        } catch (IOException e) {
                            log.error("이미지 로딩 중 오류 발생", e);
                        }
                    }
                    log.info(dto);
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

    @Transactional
    public String updateProductColor(ProductUpdateColorDTO dto, List<MultipartFile> productImages, MultipartFile productDetailImage) {


        log.info(dto);

        ProductsColor productsColor = productsColorRepository.findByProductColorId(dto.getProductColorId())
                .orElseThrow(() -> new EntityNotFoundException());

        productsColorRepository.save(productsColor);

        List<ProductsColorSize> list = productsColorSizeRepository.findByProductColorId(dto.getProductColorId());

        for(ProductSizeDTO sizeDTO : dto.getSize() ) {
            Size size = Size.valueOf(sizeDTO.getSize());
            ProductsColorSize productsColorSize = productsColorSizeRepository.findByProductColorIdAndSize(dto.getProductColorId(), size);
            if (productsColorSize != null) {
                ProductsStock productsStock = productsStockRepository.findByProductColorSizeId(productsColorSize.getProductColorSizeId());
                productsStock.setStock(sizeDTO.getStock());
                productsStockRepository.save(productsStock);
            } else {
                productsColorSize = ProductsColorSize.builder()
                        .productsColor(productsColor)
                        .size(Size.valueOf(sizeDTO.getSize()))
                        .build();
                productsColorSizeRepository.save(productsColorSize);
                ProductsStock productsStock = ProductsStock.builder()
                        .productsColorSize(productsColorSize)
                        .stock(sizeDTO.getStock())
                        .build();
                productsStockRepository.save(productsStock);
            }
            List<ProductsColorSize> toDeleteColorSizes = new ArrayList<>();
            List<ProductsStock> toDeleteStocks = new ArrayList<>();

            for (ProductsColorSize colorSize : list) {
                boolean found = false;
                for (ProductSizeDTO NewSizeDTO : dto.getSize()) {
                    if (colorSize.getSize() == Size.valueOf(NewSizeDTO.getSize())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    ProductsStock productsStock = productsStockRepository.findByProductColorSizeId(colorSize.getProductColorSizeId());
                    if (productsStock != null) {
                        toDeleteStocks.add(productsStock);
                    }
                    toDeleteColorSizes.add(colorSize);
                }
            }
            for (ProductsColorSize colorSize : toDeleteColorSizes) {
                productsColorSizeRepository.delete(colorSize);
            }

            for (ProductsStock productsStock : toDeleteStocks) {
                productsStockRepository.delete(productsStock);
            }

        }


        List<ProductsImage> productsImageList = productsImageRepository.findByProductColorId(dto.getProductColorId());
        for (ProductsImage productsImage : productsImageList) {
            String filePath = uploadPath + File.separator + productsImage.getUuid() + "_" + productsImage.getFileName();
            try {
                Files.deleteIfExists(Paths.get(filePath));
                productsImageRepository.deleteById(productsImage.getProductImageId());
            } catch (IOException e) {
                log.error("Failed to delete image file: " + filePath, e);
            }
        }


        ProductsDetailImage productsDetailImage = productsDetailImageRepository.findOneByProductColorId(dto.getProductColorId());
        String filePath = uploadPath + File.separator + productsDetailImage.getUuid() + "_" + productsDetailImage.getFileName();
        try {
            Files.deleteIfExists(Paths.get(filePath));
            productsDetailImageRepository.deleteById(productsDetailImage.getProductsDetailImageId());
        } catch (IOException e) {
            log.error("Failed to delete image file: " + filePath, e);
        }

        boolean isFirstImage = true;
        for (MultipartFile image : productImages) {
            String uuid = saveImage(image);
            ProductsImage productsImage = ProductsImage.builder()
                    .productsColor(productsColor)
                    .fileName(image.getOriginalFilename())
                    .uuid(uuid)
                    .isMain(isFirstImage)
                    .build();
            productsImageRepository.save(productsImage);
            isFirstImage = false;
        }

        String uuid = saveImage(productDetailImage);
        ProductsDetailImage image = ProductsDetailImage.builder()
                .productsColor(productsColor)
                .fileName(productDetailImage.getOriginalFilename())
                .uuid(uuid)
                .build();
        productsDetailImageRepository.save(image);

        return "상품 색상 및 수량 수정 완료";
    }

    @Override
    public List<ProductColorListBySellerDTO> getProductColorListBySeller(String email) {
        Users users = userRepository.findByEmail(email).orElse(null);

        List<ProductsSeller> list = productsSellerRepository.findByUsers(users);
        List<ProductColorListBySellerDTO> dtoList = new ArrayList<>();

        for(ProductsSeller productsSeller: list){
            List<ProductsColor> productsColor = productsColorRepository.findByProductId(productsSeller.getProducts().getProductId());
            for(ProductsColor color : productsColor) {
                ProductColorListBySellerDTO dto = new ProductColorListBySellerDTO();
                dto.setProductColorId(color.getProductColorId());
                dto.setColor(color.getColor());
                dto.setProductNumber(productsSeller.getProducts().getProductNumber());
                dto.setProductName(productsSeller.getProducts().getName());
                dtoList.add(dto);
            }
        }


        return dtoList;
    }

    @Override
    public ProductColorUpdateGetInfoDTO getProductColorSizeInfo(Long productColorId) throws IOException {
        ProductColorUpdateGetInfoDTO dto = new ProductColorUpdateGetInfoDTO();
        List<ProductSizeDTO> dtoList = new ArrayList<>();
        List<ProductsColorSize> productsColorSize = productsColorSizeRepository.findByProductColorId(productColorId);
        for(ProductsColorSize size : productsColorSize){
            ProductSizeDTO productSizeDTO = new ProductSizeDTO();
            ProductsStock productsStock = productsStockRepository.findByProductColorSizeId(size.getProductColorSizeId());
            productSizeDTO.setSize(size.getSize().name());
            productSizeDTO.setStock(productsStock.getStock());
            dtoList.add(productSizeDTO);
        }

        dto.setColor(productsColorSize.get(0).getProductsColor().getColor());
        dto.setSize(dtoList);

        List<ProductsImage> productsImage = productsImageRepository.findByProductColorId(productColorId);

        List<ProductImageInfoDTO> imageList = new ArrayList<>();
        for(ProductsImage image : productsImage){
            ProductImageInfoDTO productImageInfoDTO = new ProductImageInfoDTO();
            productImageInfoDTO.setFilename(image.getFileName());
            byte[] imageData = getImage(image.getUuid(),image.getFileName());
            productImageInfoDTO.setProductColorImage(imageData);
            imageList.add(productImageInfoDTO);
        }
        dto.setImages(imageList);
        ProductImageInfoDTO productImageInfoDTO = new ProductImageInfoDTO();
        ProductsDetailImage productsDetailImage = productsDetailImageRepository.findOneByProductColorId(productColorId);
        productImageInfoDTO.setFilename(productsDetailImage.getFileName());
        byte[] imageData = getImage(productsDetailImage.getUuid(),productsDetailImage.getFileName());
        productImageInfoDTO.setProductColorImage(imageData);
        dto.setDetailImage(productImageInfoDTO);
        return dto;
    }

    @Override
    public String updateProduct(ProductUpdateDTO dto) {
        // productId로 상품을 찾음
        Products products = productsRepository.findById(dto.getProductId())
                .orElseThrow(() -> new ProductNotFoundException());

        // 상품 업데이트
        products.setName(dto.getName());
        products.setPrice(dto.getPrice());
        products.setIsSale(dto.isSale());
        products.setPriceSale(dto.getPriceSale());
        products.setCategory(dto.getCategory());
        products.setCategorySub(dto.getCategorySub());
        products.setSeason(dto.getSeason());
        dto.setProductNumber(dto.getProductNumber());

        productsRepository.save(products);

        return "상품 수정 완료";
    }


    @Override
    public void deleteProduct(Long productId) {

        productsRepository.deleteById(productId);
    }

    @Override
    public void deleteProductColor(Long productColorId) {

        productsColorRepository.deleteById(productColorId);
    }

    @Override
    public String createProductColor(ProductRegisterColorDTO dto, List<MultipartFile> productImages, MultipartFile productDetailImage) {


        Products products = productsRepository.findById(dto.getProductId()).orElse(null);

        if (products == null) {
            return "상품이 존재하지 않습니다."; // 상품이 없을 때 메시지 반환
        }

        ProductsColor productsColor = ProductsColor.builder()
                .products(products)
                .color(dto.getColor())
                .build();
        productsColorRepository.save(productsColor);




        for (ProductSizeDTO size : dto.getSize()) {
            ProductsColorSize productsColorSize = ProductsColorSize.builder()
                    .productsColor(productsColor)
                    .size(Size.valueOf(size.getSize()))
                    .build();
            productsColorSizeRepository.save(productsColorSize);

            ProductsStock productsStock = ProductsStock.builder()
                    .productsColorSize(productsColorSize)
                    .stock(size.getStock())
                    .build();
            productsStockRepository.save(productsStock);

        }

        if (productImages != null && !productImages.isEmpty()) {
            boolean isFirstImage = true;
            for (MultipartFile image : productImages) {
                String uuid = saveImage(image);
                boolean isMain = isFirstImage;
                isFirstImage = false; // 다음 이미지는 첫 번째 이미지가 아님
                ProductsImage productsImage = ProductsImage.builder()
                        .productsColor(productsColor)
                        .fileName(image.getOriginalFilename())
                        .uuid(uuid)
                        .isMain(isMain)
                        .build();
                productsImageRepository.save(productsImage);
            }
        }

        if (productDetailImage != null && !productDetailImage.isEmpty()) {
            String uuid = saveImage(productDetailImage);

            ProductsDetailImage productsDetailImage = ProductsDetailImage.builder()
                    .productsColor(productsColor)
                    .fileName(productDetailImage.getOriginalFilename())
                    .uuid(uuid)
                    .build();
            productsDetailImageRepository.save(productsDetailImage);
        }
        return "상품 색상이 성공적으로 등록되었습니다.";
    }



    private String saveImage(MultipartFile image) {
        String fileName = image.getOriginalFilename();
        String uuid = UUID.randomUUID().toString();
        Path savePath = Paths.get(uploadPath, uuid + "_" + fileName);
        try {
            // 파일을 지정된 경로에 저장
            image.transferTo(savePath.toFile());
            log.info("이미지 업로드 성공");
        } catch (io.jsonwebtoken.io.IOException e) {
            // 파일 저장 또는 썸네일 생성 중 오류가 발생할 경우
            log.error("파일 저장하는 도중 오류가 발생했습니다: ", e);
            throw new RuntimeException("File processing error", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return uuid;
    }

    @Override
    public ProductInfoDTO getProductColorInfo(Long productColorId) {
        log.info(productColorId);

        Optional<ProductsColor> result = productsColorRepository.findById(productColorId);
        ProductsColor products = result.orElseThrow(() -> new ProductNotFoundException()); // Products 객체 생성
        List<ProductsColor> resultList = productsColorRepository.findByProductId(products.getProducts().getProductId());
        ProductInfoDTO productInfoDTO = modelMapper.map(products, ProductInfoDTO.class); // dto 매핑

        if (resultList.size() > 1) {
            productInfoDTO.setBooleanColor(true);
            List<String> colors = new ArrayList<>();
            for (ProductsColor productsColor : resultList) {
                colors.add(productsColor.getColor());
            }
            productInfoDTO.setColorType(colors);
        } else {
            productInfoDTO.setBooleanColor(false); // resultList의 크기가 1 이하인 경우 false로 설정
            // 필요한 경우 다른 로직 추가
        }

        productInfoDTO.setProductId(products.getProducts().getProductId());
        productInfoDTO.setProductColorId(productColorId);
        productInfoDTO.setBrandName(products.getProducts().getBrandName());
        productInfoDTO.setName(products.getProducts().getName());
        productInfoDTO.setCategory(products.getProducts().getCategory());
        productInfoDTO.setCategorySub(products.getProducts().getCategorySub());
        productInfoDTO.setPrice(products.getProducts().getPrice());
        productInfoDTO.setPriceSale(products.getProducts().getPriceSale());
        productInfoDTO.setProductNumber(products.getProducts().getProductNumber());
        productInfoDTO.setSeason(products.getProducts().getSeason());
        productInfoDTO.setSale(products.getProducts().getIsSale());


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        log.info(authentication);
        String currentUserName = authentication.getName();

        log.info(currentUserName);


        Users users = userRepository.findByEmail(currentUserName)
                .orElse(null);

        if (users != null) {
            Long userId = users.getUserId();
            LikeProducts likeProducts = likeRepository.findByProductColorIdAndUserId(productColorId, userId);
            productInfoDTO.setLike(likeProducts != null ? likeProducts.isLike() : false);
        } else {
            productInfoDTO.setLike(false);
        }

        ProductsStar productsStar = productsStarRepository.findByProductColorId(productColorId).orElse(null);

        productInfoDTO.setStarAvg(productsStar != null ? productsStar.getStarAvg() : 0);

        ProductsLike productsLike = productsLikeRepository.findByProductColorId(productColorId).orElse(null);

        productInfoDTO.setLikeIndex(productsLike != null ? productsLike.getLikeIndex() : 0);

        productInfoDTO.setReviewIndex(
                reviewRepository.countByProductColorId(productColorId) != 0
                        ? reviewRepository.countByProductColorId(productColorId)
                        : 0);

        List<ProductsImage> productImages = productsImageRepository.findByProductColorId(products.getProductColorId());
        List<byte[]> imageBytesList = new ArrayList<>();
        for (ProductsImage productsImage : productImages) {
            try {
                byte[] imageData = getImage(productsImage.getUuid(), productsImage.getFileName());
                imageBytesList.add(imageData);
            } catch (IOException e) {
                // 예외 처리
                e.printStackTrace();
            }
        }
        productInfoDTO.setProductImages(imageBytesList);

        return productInfoDTO;
    }


    @Override
    public ProductCreateGetInfoDTO getProductInfo(Long productId) throws IOException {

        Optional<Products> result = productsRepository.findById(productId);
        Products products = result.orElseThrow(() -> new ProductNotFoundException()); // Products 객체 생성
        List<ProductsColor> resultList = productsColorRepository.findByProductId(productId);
        ProductCreateGetInfoDTO dto = new ProductCreateGetInfoDTO();

        if (resultList.size() >= 1) {
            List<String> colors = new ArrayList<>();
            for (ProductsColor productsColor : resultList) {
                colors.add(productsColor.getColor());
            }
            dto.setColorType(colors);
        }

        dto.setBrandName(products.getBrandName());
        dto.setName(products.getName());
        dto.setCategory(products.getCategory());
        dto.setCategorySub(products.getCategorySub());
        dto.setPrice(products.getPrice());
        dto.setPriceSale(products.getPriceSale());
        dto.setProductNumber(products.getProductNumber());
        dto.setSeason(products.getSeason());
        dto.setSale(products.getIsSale());


        return dto;
    }

    @Override
    public ProductColorInfoCardDTO getProductCardInfo(Long productColorId) {
        ProductsColor productsColor = productsColorRepository.findById(productColorId).orElse(null);
        ProductsLike productsLike = productsLikeRepository.findByProductColorId(productColorId).orElse(null);
        ProductsStar productsStar = productsStarRepository.findByProductColorId(productColorId).orElse(null);
        ProductColorInfoCardDTO dto = ProductColorInfoCardDTO.builder()
                .productColorId(productColorId)
                .name(productsColor.getProducts().getName())
                .color(productsColor.getColor())
                .brandName(productsColor.getProducts().getBrandName())
                .category(productsColor.getProducts().getCategory())
                .categorySub(productsColor.getProducts().getCategorySub())
                .isSale(productsColor.getProducts().getIsSale())
                .price(productsColor.getProducts().getPrice())
                .priceSale(productsColor.getProducts().getPriceSale())
                .starAvg(productsStar != null ? productsStar.getStarAvg() : 0)
                .likeIndex(productsLike != null ? productsLike.getLikeIndex() : 0)
                .build();
        ProductsImage productsImage = productsImageRepository.findFirstByProductColorId(productColorId);
        if (productsImage != null) {
            try {
                byte[] imageData = getImage(productsImage.getUuid(), productsImage.getFileName());
                dto.setProductImage(imageData);
            } catch (IOException e) {
                // 예외 처리
                e.printStackTrace();
                dto.setProductImage(null);
            }
        } else {
            dto.setProductImage(null);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String currentUserName = authentication.getName();

        Users users = userRepository.findByEmail(currentUserName)
                .orElse(null);

        if (users != null) {
            LikeProducts likeProducts = likeRepository.findByProductColorIdAndUserId(productColorId, users.getUserId());
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
    public ProductDetailImagesDTO getProductDetailImages(Long productColorId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductsDetailImage> productDetailImages = productsDetailImageRepository
                .findByProductColorId(productColorId, pageable);
        long count = productDetailImages.getTotalElements();

        Page<byte[]> detailImageBytesPage = productDetailImages.map(productsImage -> {
            try {
                byte[] imageData = getImage(productsImage.getUuid(), productsImage.getFileName());
                return imageData;
            } catch (IOException e) {
                // 예외 처리
                e.printStackTrace();
                return null; // 예외 발생 시 null 반환
            }
        });

        ProductDetailImagesDTO productDetailImagesDTO = new ProductDetailImagesDTO();
        productDetailImagesDTO.setProductImages(detailImageBytesPage);
        productDetailImagesDTO.setImagesCount(count);
        log.info("카운트" + count);
        log.info("카운트1" + productDetailImages);
        return productDetailImagesDTO;
    }

    @Override
    public List<ProductOtherColorImagesDTO> getRelatedColorProductsImages(Long productColorId) throws IOException {
        List<ProductOtherColorImagesDTO> colorImagesList = new ArrayList<>();

        Optional<ProductsColor> productsColorOptional = productsColorRepository.findById(productColorId);

        // 현재 상품 색상 이미지 추가
        ProductsImage currentProductsImage = productsImageRepository.findFirstByProductColorId(productColorId);
        if (currentProductsImage != null) {
            byte[] currentImageData = null;
            try {
                // 서버 파일 시스템에서 파일 존재 여부 확인
                Path filePath = Paths.get(uploadPath,
                        currentProductsImage.getUuid() + "_" + currentProductsImage.getFileName());
                if (Files.exists(filePath)) {
                    currentImageData = getImage(currentProductsImage.getUuid(), currentProductsImage.getFileName());
                    ProductOtherColorImagesDTO currentColorDTO = new ProductOtherColorImagesDTO();
                    currentColorDTO.setProductColorId(productColorId);
                    currentColorDTO.setProductColorImage(currentImageData);
                    colorImagesList.add(currentColorDTO);
                } else {
                    // 파일이 서버에 존재하지 않는 경우 예외 처리 (예: 기본 이미지 설정)
                    System.out.println("Warning: 현재 상품 색상 이미지 파일이 서버에 존재하지 않습니다. 파일 경로: " + filePath.toString());
                    // 예외 처리 로직을 추가할 수 있습니다.
                }
            } catch (IOException e) {
                // 파일을 가져오는 도중 예외가 발생한 경우 처리
                System.out.println("Error fetching current product color image: " + e.getMessage());
                // 예외 처리 로직을 추가할 수 있습니다.
            }
        }

        // 상품의 다른 색상 이미지들 추가
        List<ProductsColor> colors = productsColorRepository
                .findByProductId(productsColorOptional.get().getProducts().getProductId());

        for (ProductsColor productsColor : colors) {
            if (productsColor.getProductColorId() == productColorId) {
                continue; // 현재 상품 색상은 이미 추가했으므로 건너뜁니다.
            }

            ProductOtherColorImagesDTO dto = new ProductOtherColorImagesDTO();
            dto.setProductColorId(productsColor.getProductColorId());

            ProductsImage otherProductsImage = productsImageRepository
                    .findFirstByProductColorId(productsColor.getProductColorId());
            if (otherProductsImage != null) {
                // 서버 파일 시스템에서 파일 존재 여부 확인
                Path filePath = Paths.get(uploadPath,
                        otherProductsImage.getUuid() + "_" + otherProductsImage.getFileName());
                if (Files.exists(filePath)) {
                    byte[] otherImageData = getImage(otherProductsImage.getUuid(), otherProductsImage.getFileName());
                    dto.setProductColorImage(otherImageData);
                    colorImagesList.add(dto);
                } else {
                    // 파일이 서버에 존재하지 않는 경우 예외 처리 (예: 기본 이미지 설정)
                    log.info("Warning: 파일이 서버에 존재하지 않습니다. 데이터베이스 정보: " + otherProductsImage.getUuid() + "_"
                            + otherProductsImage.getFileName());
                    // 기본 이미지 설정 등의 로직을 추가할 수 있습니다.
                }
            }

        }
        log.info(colorImagesList);
        return colorImagesList;
    }

    @Override
    public ProductOrderBarDTO getProductOrderBar(Long productColorId) {
        ProductOrderBarDTO dto = new ProductOrderBarDTO();
        log.info("Initial DTO: " + dto);
        List<ProductStockDTO> productsStockDTO = new ArrayList<>();

        try {

            // 상품 좋아요 수 가져오기
            ProductsLike productsLike = productsLikeRepository.findByProductColorId(productColorId).orElse(null);
            dto.setLikeIndex(productsLike != null ? productsLike.getLikeIndex() : 0);

            // 상품 색상 정보 가져오기
            ProductsColor productsColor = productsColorRepository.findById(productColorId)
                    .orElseThrow(() -> new DataAccessException("Product color not found for ID: " + productColorId) {
                        // 예외 클래스를 사용자 정의하여 추가 정보를 제공할 수 있습니다.
                    });
            Products products = productsRepository.findById(productsColor.getProducts().getProductId()).orElse(null);

            dto.setPrice(products.getIsSale() ? products.getPriceSale() : products.getPrice());

            List<ProductsColor> productsColors = productsColorRepository
                    .findByProductId(productsColor.getProducts().getProductId());

            List<Long> idList = new ArrayList<>();

            for (ProductsColor pc : productsColors) {
                Long id = pc.getProductColorId();
                idList.add(id);
            }

            for (Long id : idList) {
                List<ProductsColorSize> productsColorSizes = productsColorSizeRepository.findByProductColorId(id);
                for (ProductsColorSize pcs : productsColorSizes) {
                    ProductStockDTO stockDTO = new ProductStockDTO();
                    stockDTO.setProductColorId(pcs.getProductsColor().getProductColorId());
                    stockDTO.setProductColorSizeId(pcs.getProductColorSizeId());
                    stockDTO.setColor(pcs.getProductsColor().getColor());
                    stockDTO.setSize(pcs != null ? pcs.getSize().name() : "에러");
                    ProductsStock productsStock = productsStockRepository
                            .findByProductColorSizeId(pcs.getProductColorSizeId());
                    stockDTO.setStock(productsStock != null ? productsStock.getStock() : 0);
                    productsStockDTO.add(stockDTO);
                }
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
                LikeProducts likeProducts = likeRepository.findByProductColorIdAndUserId(productColorId, userId);
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
            List<ProductsColor> productsColors = productsColorRepository.findByProductId(products.getProductId());
            for (ProductsColor productsColor : productsColors) {
                ProductColorInfoCardDTO dto = new ProductColorInfoCardDTO();
                dto.setBrandName(products.getBrandName());
                dto.setName(products.getName());
                dto.setCategory(products.getCategory());
                dto.setCategorySub(products.getCategorySub());
                dto.setPrice(products.getPrice());
                dto.setPriceSale(products.getPriceSale());
                dto.setSale(products.getIsSale());
                dto.setColor(productsColor.getColor());
                if (users != null) {
                    Long userId = users.getUserId();
                    LikeProducts likeProducts = likeRepository
                            .findByProductColorIdAndUserId(productsColor.getProductColorId(), userId);
                    dto.setLike(likeProducts != null ? likeProducts.isLike() : false);
                } else {
                    dto.setLike(false);
                }
                ProductsLike productsLike = productsLikeRepository
                        .findByProductColorId(productsColor.getProductColorId()).orElse(null);
                ProductsStar productsStar = productsStarRepository
                        .findByProductColorId(productsColor.getProductColorId()).orElse(null);
                ProductsImage productsImage = productsImageRepository
                        .findFirstByProductColorId(productsColor.getProductColorId());
                log.info(productsImage);
                if (productsImage != null) {
                    try {
                        byte[] imageData = getImage(productsImage.getUuid(), productsImage.getFileName());
                        dto.setProductImage(imageData);
                    } catch (IOException e) {

                    }
                } else {
                    dto.setProductImage(null);
                }
                dto.setProductColorId(productsColor.getProductColorId());
                dto.setLikeIndex(productsLike != null ? productsLike.getLikeIndex() : 0);
                dto.setStarAvg(productsStar != null ? productsStar.getStarAvg() : 0);
log.info(dto.getProductImage());
                productsInfoCardDTOList.add(dto);
            }
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

            ProductsColor productsColor = productsLike.getProductsColor();

            Products products = productsColor.getProducts();

            ProductsStar productsStar = productsStarRepository.findByProductColorId(productsColor.getProductColorId())
                    .orElse(null);


            dto.setBrandName(products.getBrandName());

            dto.setName(products.getName());

            dto.setCategory(products.getCategory());

            dto.setCategorySub(products.getCategorySub());

            dto.setPrice(products.getPrice());

            dto.setPriceSale(products.getPriceSale());

            dto.setSale(products.getIsSale());
            dto.setColor(likeProducts.getProductsLike().getProductsColor().getColor());

            dto.setLike(likeProducts.isLike());
            dto.setProductColorId(productsColor.getProductColorId());
            dto.setLikeIndex(productsLike != null ? productsLike.getLikeIndex() : 0);
            dto.setStarAvg(productsStar != null ? productsStar.getStarAvg() : 0);
            ProductsImage productsImage = productsImageRepository
                    .findFirstByProductColorId(productsColor.getProductColorId());
            log.info(productsImage);
            if (productsImage != null) {
                try {
                    byte[] imageData = getImage(productsImage.getUuid(), productsImage.getFileName());
                    dto.setProductImage(imageData);
                    log.info("성공");
                } catch (IOException e) {
                    log.info(e.getMessage());
                }
            } else {
                log.info("널입니다");
                dto.setProductImage(null);
            }
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
                    Products product = productsStar.getProductsColor().getProducts();
                    Long productColorId = productsStar.getProductsColor().getProductColorId();

                    ProductsStarRankListDTO dto = ProductsStarRankListDTO.builder()
                            .productColorId(productColorId)
                            .brandName(product.getBrandName())
                            .price(product.getPrice())
                            .priceSale(product.getPriceSale())
                            .isSale(product.getIsSale())
                            .category(product.getCategory())
                            .categorySub(product.getCategorySub())
                            .name(product.getName())
                            .build();

                    if (users != null) {
                        LikeProducts likeProducts = likeRepository.findByProductColorIdAndUserId(productColorId, users.getUserId());
                        dto.setLike(likeProducts != null ? likeProducts.isLike() : false);
                    }else {
                        dto.setLike(false);
                    }

                    log.info(dto.isLike());

                    ProductsImage productImage = productsImageRepository.findByProductsColorProductColorIdAndIsMainTrue(productColorId);
                    if (productImage != null) {
                        try {
                            String filePath = uploadPath + productImage.getUuid() + "_" + productImage.getFileName();
                            Path path = Paths.get(filePath);
                            if (Files.exists(path)) {
                                byte[] imageData = Files.readAllBytes(path);
                                dto.setProductImage(imageData);
                            } else {
                                log.warn("이미지 파일이 존재하지 않습니다: " + filePath);
                            }
                        } catch (IOException e) {
                            log.error("이미지 로딩 중 오류 발생", e);
                        }
                    }
                    log.info(dto);
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

                    Products product = productsStar.getProductsColor().getProducts();
                    Long productColorId = productsStar.getProductsColor().getProductColorId();

                    ProductsStarRankListDTO dto = ProductsStarRankListDTO.builder()
                            .productColorId(productColorId)
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
                        LikeProducts likeProducts = likeRepository.findByProductColorIdAndUserId(productColorId, users.getUserId());
                        dto.setLike(likeProducts != null ? likeProducts.isLike() : false);
                    }else {
                        dto.setLike(false);
                    }

                    log.info(dto.isLike());

                    ProductsImage productImage = productsImageRepository.findByProductsColorProductColorIdAndIsMainTrue(productColorId);
                    if (productImage != null) {
                        try {
                            String filePath = uploadPath + productImage.getUuid() + "_" + productImage.getFileName();
                            Path path = Paths.get(filePath);
                            if (Files.exists(path)) {
                                byte[] imageData = Files.readAllBytes(path);
                                dto.setProductImage(imageData);
                            } else {
                                log.warn("이미지 파일이 존재하지 않습니다: " + filePath);
                            }
                        } catch (IOException e) {
                            log.error("이미지 로딩 중 오류 발생", e);
                        }
                    }
                    log.info(dto);
                    return dto;
                })
                .limit(10)
                .collect(Collectors.toList());
    }



}