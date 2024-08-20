package com.daewon.xeno_backend.service;

import com.daewon.xeno_backend.domain.*;
import com.daewon.xeno_backend.dto.cart.AddToCartDTO;
import com.daewon.xeno_backend.dto.cart.CartDTO;
import com.daewon.xeno_backend.dto.cart.CartSummaryDTO;
import com.daewon.xeno_backend.exception.UserNotFoundException;
import com.daewon.xeno_backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final UserRepository userRepository;
    private final ProductsRepository productsRepository;
    private final CartRepository cartRepository;
    private final ProductsColorSizeRepository productsColorSizeRepository;
    private final ProductsImageRepository productsImageRepository;

    @Value("${uploadPath}")
    private String uploadPath;

    @Override
    public void addToCart(List<AddToCartDTO> addToCartDTOList) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        log.info(authentication);
        String currentUserName = authentication.getName();

        log.info(currentUserName);

        Users users = userRepository.findByEmail(currentUserName)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없음"));

        Cart cart = new Cart();

        for(AddToCartDTO addToCartDTO: addToCartDTOList) {

            cart = cartRepository.findByProductColorSizeIdAndUser(addToCartDTO.getProductColorSizeId(),users.getUserId()).orElse(null);

            ProductsColorSize productsColorSize = productsColorSizeRepository.findById(addToCartDTO.getProductColorSizeId()).orElse(null);
            ProductsImage productsImage = productsImageRepository.findFirstByProductColorId(productsColorSize.getProductsColor().getProductColorId());
            if(cart == null) {
                cart = Cart.builder()
                        .price(addToCartDTO.getPrice())
                        .productsColorSize(productsColorSize)
                        .quantity(addToCartDTO.getQuantity())
                        .user(users)
                        .productsImage(productsImage)
                        .build();

                cartRepository.save(cart);
            } else {
                cart.setQuantity(cart.getQuantity()+addToCartDTO.getQuantity());
                cart.setPrice(cart.getPrice()+ addToCartDTO.getPrice());
                cartRepository.save(cart);
            }
        }
    }

    @Override
    public List<CartDTO> getCartItems(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("해당하는 유저를 찾을 수 없습니다."));
        List<Cart> carts = cartRepository.findByUser(user);
        return carts.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public void updateCartItem(Long userId, Long cartId, Long quantity, Long price) {
        Cart cart = cartRepository.findByCartIdAndUserUserId(cartId, userId)
                .orElseThrow(() -> new RuntimeException("해당 사용자의 장바구니 상품을 찾을 수 없습니다."));

        // 수량이 0이면 DB에서 해당하는 cart 삭제
        if (quantity <= 0) {
            cartRepository.delete(cart);
            return;
        }

        log.info(quantity);
        cart.setQuantity(quantity);
        cart.setPrice(price * quantity);
        cartRepository.save(cart);
    }

    @Override
    public boolean removeFromCart(Long cartId) {
        if (cartRepository.existsById(cartId)) {
            cartRepository.deleteById(cartId);
            return true;
        }
        return false;
    }

    @Override
    public CartSummaryDTO getCartSummary(Long userId) {
        List<Cart> carts = cartRepository.findByUser(userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found")));

        Long totalPrice = carts.stream()
                .mapToLong(cart -> cart.getPrice())
                .sum();
        int totalProductIndex = carts.stream()
                .mapToInt(cart -> cart.getQuantity().intValue())
                .sum();
        return new CartSummaryDTO(totalProductIndex, totalPrice);
    }

    @Override
    public CartDTO convertToDTO(Cart cart) {
        CartDTO cartDTO = new CartDTO();
        cartDTO.setCartId(cart.getCartId());
        cartDTO.setProductsColorSizeId(cart.getProductsColorSize().getProductColorSizeId());
        cartDTO.setQuantity(cart.getQuantity());
        cartDTO.setAmount(cart.getPrice());
        cartDTO.setBrandName(cart.getProductsColorSize().getProductsColor().getProducts().getBrandName());
        cartDTO.setSale(cart.getProductsColorSize().getProductsColor().getProducts().getIsSale());
        cartDTO.setPrice(cart.getPrice()/cart.getQuantity());
        cartDTO.setProductName(cart.getProductsColorSize().getProductsColor().getProducts().getName());
        cartDTO.setColor(cart.getProductsColorSize().getProductsColor().getColor());
        cartDTO.setSize(String.valueOf(cart.getProductsColorSize().getSize()));

        ProductsImage image = cart.getProductsImage();
        if (image != null) {
            try {
                byte[] imageData = getImage(image.getUuid(), image.getFileName());
                cartDTO.setProductImage(imageData);
            } catch (IOException e) {
                log.error("이미지 로딩 실패: " + e.getMessage());
            }
        }

        return cartDTO;
    }

    public byte[] getImage(String uuid, String fileName) throws IOException {
        String filePath = uploadPath + uuid + "_" + fileName;
        // 파일을 바이트 배열로 읽기
        Path path = Paths.get(filePath);
        byte[] image = Files.readAllBytes(path);
        return image;
    }
}
