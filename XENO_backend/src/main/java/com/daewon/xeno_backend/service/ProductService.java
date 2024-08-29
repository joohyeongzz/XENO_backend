
package com.daewon.xeno_backend.service;


import com.daewon.xeno_backend.domain.Products;

import com.daewon.xeno_backend.dto.UploadImageReadDTO;
import com.daewon.xeno_backend.dto.page.PageInfinityResponseDTO;
import com.daewon.xeno_backend.dto.page.PageRequestDTO;
import com.daewon.xeno_backend.dto.page.PageResponseDTO;
import com.daewon.xeno_backend.dto.product.*;
import org.springframework.core.io.InputStreamResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ProductService {


//    String asd(MultipartFile file);

    void uploadImages(String productNumber, List<MultipartFile> productImages ,MultipartFile productDetailImage);

    void updateProductImages(String productNumber, List<MultipartFile> productImages ,MultipartFile productDetailImage) throws Exception;

    ProductInfoDTO getProductInfo(Long productId) throws IOException;

    ProductColorInfoCardDTO getProductCardInfo(Long productId);

    ProductDetailImagesDTO getProductDetailImages(Long productId, int page, int size);

    ProductOrderBarDTO getProductOrderBar(Long productId);

    List<ProductColorInfoCardDTO> getProductsInfoByCategory(String categoryId, String categorySubId);

    List<ProductColorInfoCardDTO> getLikedProductsInfo();

    List<ProductsStarRankListDTO> getranktop10(String category);

    PageInfinityResponseDTO<ProductsStarRankListDTO> getrankTop50(String category, PageRequestDTO pageRequestDTO);


    List<ProductListBySellerDTO> getProductListBySeller(String email);

//    PageResponseDTO<ProductsSearchDTO> productCategorySearch(String category, PageRequestDTO pageRequestDTO);

    PageResponseDTO<ProductsSearchDTO> BrandNameOrNameOrCategoryOrCategorysubSearch(String keyword,PageRequestDTO pageRequestDTO);

    void saveProductsFromExcel(MultipartFile excel);

    List<UploadImageReadDTO> getUploadImageAll();

    UploadImageReadDTO getUploadImageByProductNumber(String productNumber);



}
