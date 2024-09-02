package com.daewon.xeno_backend.service;

import com.daewon.xeno_backend.domain.Cart;
import com.daewon.xeno_backend.dto.cart.AddToCartDTO;
import com.daewon.xeno_backend.dto.cart.CartDTO;
import com.daewon.xeno_backend.dto.cart.CartSummaryDTO;

import java.util.List;

public interface CartService {

    void addToCart(List<AddToCartDTO> addToCartDTOList);

    List<CartDTO> getCartItems(Long userId);

    void updateCartItem(Long userId, Long cartId, Long quantity, Long price);

    boolean removeFromCart(Long cartId);

    CartSummaryDTO getCartSummary(Long userId);

    CartDTO convertToDTO(Cart cart);
}
