package com.daewon.xeno_backend.service;

import com.daewon.xeno_backend.domain.*;
import com.daewon.xeno_backend.domain.auth.Users;
import com.daewon.xeno_backend.dto.cart.AddToCartDTO;
import com.daewon.xeno_backend.dto.cart.CartDTO;
import com.daewon.xeno_backend.dto.cart.CartSummaryDTO;
import com.daewon.xeno_backend.exception.UserNotFoundException;
import com.daewon.xeno_backend.repository.*;
import com.daewon.xeno_backend.repository.Products.ProductsImageRepository;
import com.daewon.xeno_backend.repository.Products.ProductsOptionRepository;
import com.daewon.xeno_backend.repository.Products.ProductsRepository;
import com.daewon.xeno_backend.repository.auth.UserRepository;
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
    private final ProductsOptionRepository productsOptionRepository;
    private final ProductsImageRepository productsImageRepository;

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

            cart = cartRepository.findByProductOptionIdAndUser(addToCartDTO.getProductOptionId(),users.getUserId()).orElse(null);

            ProductsOption productsOption = productsOptionRepository.findById(addToCartDTO.getProductOptionId()).orElse(null);
            ProductsImage productsImage = productsImageRepository.findByProductId(productsOption.getProducts().getProductId());
            if(cart == null) {
                cart = Cart.builder()
                        .price(addToCartDTO.getPrice())
                        .productsOption(productsOption)
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
        cartDTO.setProductsOptionId(cart.getProductsOption().getProductOptionId());
        cartDTO.setQuantity(cart.getQuantity());
        cartDTO.setAmount(cart.getPrice());
        cartDTO.setBrandName(cart.getProductsOption().getProducts().getBrandName());
        cartDTO.setSale(cart.getProductsOption().getProducts().getIsSale());
        cartDTO.setPrice(cart.getPrice()/cart.getQuantity());
        cartDTO.setProductName(cart.getProductsOption().getProducts().getName());
        cartDTO.setColor(cart.getProductsOption().getProducts().getColor());
        cartDTO.setSize(String.valueOf(cart.getProductsOption().getSize()));


        return cartDTO;
    }


}
