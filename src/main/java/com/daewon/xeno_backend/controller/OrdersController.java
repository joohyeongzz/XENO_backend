package com.daewon.xeno_backend.controller;

import com.daewon.xeno_backend.domain.auth.Customer;
import com.daewon.xeno_backend.dto.auth.AuthSigninDTO;
import com.daewon.xeno_backend.dto.order.*;
import com.daewon.xeno_backend.dto.page.PageInfinityResponseDTO;
import com.daewon.xeno_backend.dto.page.PageRequestDTO;
import com.daewon.xeno_backend.exception.UserNotFoundException;
import com.daewon.xeno_backend.repository.auth.CustomerRepository;
import com.daewon.xeno_backend.repository.auth.UserRepository;
import com.daewon.xeno_backend.service.ExcelService;
import com.daewon.xeno_backend.service.OrdersService;
import com.daewon.xeno_backend.utils.JWTUtil;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@Log4j2
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrdersController {

    private final OrdersService ordersService;
    private final JWTUtil jwtUtil;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final ExcelService excelService;

    @GetMapping
    public ResponseEntity<?> getAllOrders(@RequestHeader("Authorization") String token) {
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

            List<OrdersListDTO> ordersList = ordersService.getAllOrders(userId);

            log.info("주문 목록: " + ordersList);

            return ResponseEntity.ok(ordersList);
        } catch (JwtException e) {
            return ResponseEntity.status(401).body("토큰이 유효하지 않습니다.");
        }
    }

    @PostMapping(produces = "application/json")
    public ResponseEntity<?> createOrder(
                                        @RequestBody List<OrdersDTO> ordersDTO,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String userEmail = userDetails.getUsername();

            Customer customer = customerRepository.findByUserId(userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found")).getUserId())
                .orElseThrow(() -> new UserNotFoundException("Customer not found"));

            int usedPoint = ordersDTO.stream().mapToInt(OrdersDTO::getUsedPoint).sum();
            if (usedPoint > customer.getPoint()) {
                return ResponseEntity.status(400).body("사용 가능한 적립금이 부족합니다.");
            }
            customer.setPoint(customer.getPoint() - usedPoint);
            customerRepository.save(customer);

            // 상품 가격에서 사용한 적립금만큼 차감
            ordersDTO.forEach(dto -> dto.setAmount(dto.getAmount() - dto.getUsedPoint()));

            List<OrdersDTO> createdOrder = ordersService.createOrders(ordersDTO, userEmail);
            return ResponseEntity.ok(createdOrder);
        } catch (Exception e) {
            return ResponseEntity.status(404).body("해당하는 상품 또는 재고가 없습니다.");
        }
    }

    @GetMapping("/latestReq")
    public ResponseEntity<String> getLatestReq(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        String latestReq = ordersService.getLatestReqForUser(email);

        return ResponseEntity.ok(latestReq);
    }

    //  프론트에서 address, phoneNumber 값을 보내주면 해당하는 user의 address, phoneNumber 추가됨.
    @PostMapping("/delivery")
    public ResponseEntity<String> updateDeliveryInfo(@RequestBody DeliveryOrdersDTO deliveryOrdersDTO,
                                                     @AuthenticationPrincipal UserDetails userDetails) {
        try {
            ordersService.updateUserDeliveryInfo(
                    userDetails.getUsername(),
                    deliveryOrdersDTO.getAddress(),
                    deliveryOrdersDTO.getPhoneNumber()
            );

            return ResponseEntity.ok("배송 정보를 업데이트 했습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(400).body("알맞은 주소와 휴대폰 번호를 입력해주세요");
        }
    }

    // ** 주의사항 ** 주문한 사람의 토큰값이 아니면 Exception에 걸림.
    @GetMapping("/confirm")
    public ResponseEntity<?> confirmOrders(@RequestParam Long orderId, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            OrdersConfirmDTO ordersConfirmDTO = ordersService.confirmOrder(orderId, userDetails.getUsername());

            return ResponseEntity.ok(ordersConfirmDTO);
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(403).body("접근 권한이 없습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(404).body("주문내역을 찾을 수 없습니다.");
        }
    }


    @GetMapping("/list")
    public ResponseEntity<PageInfinityResponseDTO<OrdersCardListDTO>> getOrderCardList(@AuthenticationPrincipal UserDetails userDetails, PageRequestDTO pageRequestDTO) {
        try {
            String userEmail = userDetails.getUsername();

            log.info("orderUserEmail : " + userEmail);
            PageInfinityResponseDTO<OrdersCardListDTO> orderCardList = ordersService.getOrderCardList(pageRequestDTO,userEmail);
            log.info(orderCardList);
            return ResponseEntity.ok(orderCardList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/seller/list")
    public ResponseEntity<List<OrderInfoBySellerDTO>> getOrderListBySeller(@AuthenticationPrincipal UserDetails userDetails, PageRequestDTO pageRequestDTO) {
        try {
            String userEmail = userDetails.getUsername();

            log.info("orderUserEmail : " + userEmail);
            List<OrderInfoBySellerDTO> orderList = ordersService.getOrderListBySeller(userEmail);
            log.info(orderList);
            return ResponseEntity.ok(orderList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/seller/status/update",produces = "application/json")
    public ResponseEntity<?> updateOrderStatusBySeller(@RequestBody OrdersStatusUpdateDTO dto) {
        try {

            log.info(dto);
             ordersService.updateOrderStatusBySeller(dto);

            return ResponseEntity.ok("\"성공\"");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @PutMapping(value = "/tracking", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateOrder(@RequestPart(name = "excel") MultipartFile excel) {
        // Check if the file is empty
        if (excel.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("엑셀 파일이 업로드되지 않았습니다.");
        }

        // Validate file type
        String contentType = excel.getContentType();
        if (contentType == null || !contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("유효하지 않은 파일 형식입니다. 엑셀 파일만 업로드할 수 있습니다.");
        }

        try {
            // Process the file
            excelService.parseOrderExcelFile(excel);
            return ResponseEntity.ok("성공");
        } catch (IOException e) {
            // Log the exception and return a server error response
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("파일 처리 중 오류가 발생했습니다.");
        } catch (Exception e) {
            // Handle other exceptions
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("예상치 못한 오류가 발생했습니다.");
        }
    }

    @Operation(summary = "판매자 운송장 등록 엑셀 다운로드")
    @GetMapping("/download/order-shipping-excel")
    public void downloadOrderUpdateShippingExcel(HttpServletResponse response) throws IOException {
        byte[] excelFile = excelService.generateOrdersExcelFile();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=order.xlsx");
        try (ServletOutputStream outputStream = response.getOutputStream()) {
            outputStream.write(excelFile);
            outputStream.flush();
        }
    }


    @Operation(summary = "판매자  주문내역 엑셀 다운로드")
    @GetMapping("/download/order-excel")
    public void downloadOrderExcelByMonth( @RequestParam int startYear,
                                           @RequestParam int startMonth,
                                           @RequestParam int startDay,
                                           @RequestParam int endYear,
                                           @RequestParam int endMonth,
                                           @RequestParam int endDay,HttpServletResponse response) throws IOException

    {
        LocalDate startDate = LocalDate.of(startYear, startMonth, startDay);
        LocalDate endDate = LocalDate.of(endYear, endMonth, endDay);
        byte[] excelFile = excelService.generateExcelForPeriod(startDate, endDate);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=order.xlsx");
        try (ServletOutputStream outputStream = response.getOutputStream()) {
            outputStream.write(excelFile);
            outputStream.flush();
        }
    }

    @GetMapping("/salesByYear")
    public ResponseEntity<Map<Integer, Boolean>> getSalesByYear() {
        Map<Integer, Boolean> salesByYear = ordersService.getSalesByYear();
        return ResponseEntity.ok(salesByYear);
    }










}

/*
    1. createOrder
    http://localhost:8090/api/orders (POST)
    [
        {
            "productOptionId": 4,
            "req": "hello",
            "quantity": 2,
            "amount": 50000
        }
    ]

    2. updateDeliveryInfo
    http://localhost:8090/api/orders/delivery (POST)
    {
        "address" : "user address",
        "phoneNumber" : "user phoneNumber"
    }

    ** 주의사항 ** 주문한 사람의 토큰값이 아니면 Exception에 걸림.
    3. confirmOrders
    http://localhost:8090/api/orders/confirms?orderId=해당하는orderId값  (GET)

    Query Params에 key : value 형태로 orderId : 해당하는값, 넣어주면 됨
 */


