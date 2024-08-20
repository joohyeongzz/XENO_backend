
package com.daewon.xeno_backend.service;


import com.daewon.xeno_backend.domain.Products;

import com.daewon.xeno_backend.dto.page.PageInfinityResponseDTO;
import com.daewon.xeno_backend.dto.page.PageRequestDTO;
import com.daewon.xeno_backend.dto.page.PageResponseDTO;
import com.daewon.xeno_backend.dto.product.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ProductService {

    ProductInfoDTO getProductColorInfo(Long productColorId) throws IOException;
    ProductCreateGetInfoDTO getProductInfo(Long productId) throws IOException;

    ProductColorInfoCardDTO getProductCardInfo(Long productColorId);

    ProductDetailImagesDTO getProductDetailImages(Long productColorId, int page, int size);

    List<ProductOtherColorImagesDTO> getRelatedColorProductsImages(Long productColorId) throws IOException;

    ProductOrderBarDTO getProductOrderBar(Long productColorId);

    List<ProductColorInfoCardDTO> getProductsInfoByCategory(String categoryId, String categorySubId);

    List<ProductColorInfoCardDTO> getLikedProductsInfo();

    List<ProductsStarRankListDTO> getranktop10(String category);

    PageInfinityResponseDTO<ProductsStarRankListDTO> getrankTop50(String category, PageRequestDTO pageRequestDTO);

    Products createProduct(ProductRegisterDTO productregisterDTO, List<MultipartFile> productImage, MultipartFile productDetailImage);

    String updateProduct(ProductUpdateDTO productUpdateDTO);

    void deleteProduct(Long productId);

    void deleteProductColor(Long productColorId);

    String createProductColor(ProductRegisterColorDTO dto, List<MultipartFile> productImage, MultipartFile productDetailImage);

    List<ProductListBySellerDTO> getProductListBySeller(String email);

//    PageResponseDTO<ProductsSearchDTO> productCategorySearch(String category, PageRequestDTO pageRequestDTO);

    PageResponseDTO<ProductsSearchDTO> BrandNameOrNameOrCategoryOrCategorysubSearch(String keyword,PageRequestDTO pageRequestDTO);

    String updateProductColor(ProductUpdateColorDTO dto, List<MultipartFile> productImage, MultipartFile productDetailImage);

    List<ProductColorListBySellerDTO> getProductColorListBySeller(String email);

    ProductColorUpdateGetInfoDTO getProductColorSizeInfo(Long productColorId) throws IOException;


}
