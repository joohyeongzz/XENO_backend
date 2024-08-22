package com.daewon.xeno_backend.controller;

import com.daewon.xeno_backend.domain.auth.Users;
import com.daewon.xeno_backend.dto.auth.AuthSigninDTO;
import com.daewon.xeno_backend.dto.cart.AddToCartDTO;
import com.daewon.xeno_backend.dto.cart.CartDTO;
import com.daewon.xeno_backend.dto.cart.CartSummaryDTO;
import com.daewon.xeno_backend.dto.cart.CartUpdateDTO;
import com.daewon.xeno_backend.repository.UserRepository;
import com.daewon.xeno_backend.service.CartService;
import com.daewon.xeno_backend.utils.JWTUtil;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Log4j2
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;

    @Value("${org.daewon.upload.path}")
    private String uploadPath;

    @PreAuthorize("hasRole('USER')")
    @PostMapping(produces = "application/json")
    public ResponseEntity<String> addToCart(@RequestBody List<AddToCartDTO> addToCartDTO) {
        try {
            // addToCartDTO를 이용한 비즈니스 로직 처리 (예: productService.addToCart(addToCartDTO);)
            cartService.addToCart(addToCartDTO);
            // 성공적으로 처리된 경우
            return ResponseEntity.ok("\"Successfully added to cart\"");

        } catch (Exception e) {
            // 오류 발생 시
            log.info(e +" dkssud ");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("\"Failed to add to cart\"");
        }
    }

    @GetMapping
    public ResponseEntity<?> getCartItems(@RequestHeader("Authorization") String token) {
        try {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            Map<String, Object> claims = jwtUtil.validateToken(token);
            Long userId = Long.parseLong(claims.get("userId").toString());

            log.info("유저 ID: " + userId);

            List<CartDTO> cartList = cartService.getCartItems(userId);

            // 현재 인증된 사용자의 ID를 가져옴
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            AuthSigninDTO authSigninDTO = (AuthSigninDTO) authentication.getPrincipal();
            Long authenticatedUserId = authSigninDTO.getUserId();

            log.info("인증된 유저 ID: " + authenticatedUserId);

            // 요청한 사용자 ID와 인증된 사용자 ID가 일치하는지 확인
            if (!userId.equals(authenticatedUserId)) {
                return ResponseEntity.status(403).body("접근 권한이 없습니다.");
            }

//            // 이미지 데이터 로드
//            for (CartDTO cart : cartList) {
//                if (cart.getImageUuid() != null && cart.getImageFileName() != null) {
//                    try {
//                        byte[] imageData = getImage(cart.getImageUuid(), cart.getImageFileName());
//                        cart.setImageData(imageData);
//                    } catch (IOException e) {
//                        log.error("이미지 로딩 실패: " + e.getMessage());
//                    }
//                }
//            }

            log.info("장바구니 목록: " + cartList);

            return ResponseEntity.ok(cartList);
        } catch (JwtException e) {
            return ResponseEntity.status(401).body("토큰이 유효하지 않습니다.");
        }
    }

    @PreAuthorize("@cartAndOrderSecurityUtils.isCartOwner(#cartUpdateDTO.cartId)")
    @PutMapping
    public ResponseEntity<?> updateCartItem(@RequestBody CartUpdateDTO cartUpdateDTO) {
        // SecurityContext에서 인증된 사용자 정보를 가져옵니다.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증되지 않은 사용자입니다.");
        }

        log.info(cartUpdateDTO);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername(); // JWT에서 사용한 email을 가져옵니다.

        Map<String, Object> Message = new HashMap<>();

        try {
            Users user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            cartService.updateCartItem(user.getUserId(), cartUpdateDTO.getCartId(), cartUpdateDTO.getQuantity(),cartUpdateDTO.getPrice());

            if (cartUpdateDTO.getQuantity() <= 0) {
                Message.put("message", "장바구니 아이템이 삭제되었습니다");
                return ResponseEntity.ok(Message);
            }
            Message.put("message", "완료되었습니다.");
            return ResponseEntity.ok(Message);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PreAuthorize("@cartAndOrderSecurityUtils.isCartOwner(#cartId)")
    @DeleteMapping
    public ResponseEntity<?> removeFromCart(@RequestParam Long cartId) {
        boolean removed = cartService.removeFromCart(cartId);

        Map<String, Object> Message = new HashMap<>();

        if (removed) {
            Message.put("message", "성공적으로 제거되었습니다.");
            return ResponseEntity.ok(Message);
        } else {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Cart item not found with id: " + cartId);
            return ResponseEntity
                    .status(404)
                    .body(errorResponse);
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getCartSummary(@RequestHeader("Authorization") String token) {
        try {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            Map<String, Object> claims = jwtUtil.validateToken(token);
            Long userId = Long.parseLong(claims.get("userId").toString());

            log.info("유저 ID: " + userId);

            // 현재 인증된 사용자의 ID를 가져옴
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            AuthSigninDTO authSigninDTO = (AuthSigninDTO) authentication.getPrincipal();
            Long authenticatedUserId = authSigninDTO.getUserId();

            log.info("인증된 유저 ID: " + authenticatedUserId);

            // 요청한 사용자 ID와 인증된 사용자 ID가 일치하는지 확인
            if (!userId.equals(authenticatedUserId)) {
                return ResponseEntity.status(403).body("접근 권한이 없습니다.");
            }

            CartSummaryDTO cartSummary = cartService.getCartSummary(userId);

            log.info("장바구니 목록: " + cartSummary);

            return ResponseEntity.ok(cartSummary);
        } catch (JwtException e) {
            return ResponseEntity.status(401).body("토큰이 유효하지 않습니다.");
        }
    }

//    public byte[] getImage(String uuid, String fileName) throws IOException {
//        String filePath = uploadPath + uuid + "_" + fileName;
//        // 파일을 바이트 배열로 읽기
//        Path path = Paths.get(filePath);
//        byte[] image = Files.readAllBytes(path);
//        return image;
//    }
}

/*
    1. addToCart (POST)
    http://localhost:8090/api/cart
    {
        "userId": 1,
        "productColorSizeId": 1,
        "productImageId": 1,
        "quantity": 2
    }

    2. getCartItems (GET)
    http://localhost:8090/api/cart/1 (1은 userId)

    3. updateCartItem (PUT)
    http://localhost:8090/api
    {
      "cartId": 4,
      "quantity": 8,
      "price": 3,
      "selected": true
    }

    4. removeFromCart (DELETE)
    http://localhost:8090/api

    formdata로
    cartId  : DB에 저장되어 있는 cartId

    5. getCartSummary (GET)
    http://localhost:8090/api/cart/summary/1 (1은 userId)
 */
