package com.daewon.xeno_backend.service;

import com.daewon.xeno_backend.domain.*;
import com.daewon.xeno_backend.domain.auth.Users;
import com.daewon.xeno_backend.dto.auth.GetOneDTO;
import com.daewon.xeno_backend.dto.order.*;
import com.daewon.xeno_backend.dto.page.PageInfinityResponseDTO;
import com.daewon.xeno_backend.dto.page.PageRequestDTO;
import com.daewon.xeno_backend.dto.product.ProductHeaderDTO;
import com.daewon.xeno_backend.exception.UserNotFoundException;
import com.daewon.xeno_backend.repository.*;
import com.daewon.xeno_backend.repository.Products.ProductsImageRepository;
import com.daewon.xeno_backend.repository.Products.ProductsOptionRepository;
import com.daewon.xeno_backend.repository.Products.ProductsBrandRepository;
import com.daewon.xeno_backend.repository.auth.UserRepository;
import io.jsonwebtoken.io.IOException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.http.HttpClient;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class OrdersServiceImpl implements OrdersService {

    private final UserRepository userRepository;
    private final OrdersRepository ordersRepository;
    private final ProductsOptionRepository productsOptionRepository;
    private final ProductsImageRepository productsImageRepository;
    private final ProductsBrandRepository productsBrandRepository;
    private final ReviewRepository reviewRepository;
    private final DeliveryTrackRepository deliveryTrackRepository;
    private final OrdersRefundRepository ordersRefundRepository;

    @Override
    public OrderDeliveryInfoReadDTO getOrderDeliveryInfo(Long userId) {
        Users user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("해당하는 유저를 찾을 수 없습니다."));
        Orders orders = ordersRepository.findLatestOrderByCustomerId(user.getCustomer().getCustomerId());

        OrderDeliveryInfoReadDTO orderDeliveryInfoReadDTO = new OrderDeliveryInfoReadDTO();

        orderDeliveryInfoReadDTO.setPhoneNumber(user.getPhoneNumber());
        orderDeliveryInfoReadDTO.setReq(orders.getReq());
        orderDeliveryInfoReadDTO.setAddress(user.getAddress());

        log.info(orderDeliveryInfoReadDTO);

        return orderDeliveryInfoReadDTO;
    };

    @Override
    public List<OrdersListDTO> getAllOrders(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        log.info("user: " + userId);
        List<Orders> orders = ordersRepository.findByCustomer(user);
        return orders.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public String getLatestReqForUser(String email) {
        return ordersRepository.findTopByCustomerEmailOrderByCreateAtDesc(email)
                .map(Orders::getReq)
                .orElse(null);
    }

    @Transactional
    @Override
    public List<OrdersDTO> createOrders(List<OrdersDTO> ordersDTO, String email) {


        Users users = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없음"));

        String orderPayId = ordersDTO.get(0).getOrderPayId();

        Long orderNumber = generateOrderNumber();

        List<Orders> savedOrders = new ArrayList<>();


        for(OrdersDTO dto : ordersDTO) {
            ProductsBrand brand = productsBrandRepository.findByProducts(findProductOption(dto.getProductOptionId()).getProducts());
            if(brand != null) {
                Orders orders = Orders.builder()
                        .orderPayId(orderPayId)
                        .orderNumber(orderNumber)
                        .productsOption(findProductOption(dto.getProductOptionId()))
                        .customer(users)
                        .brand(brand.getBrand())
                        .status("결제 완료")
                        .paymentKey(dto.getPaymentKey())
                        .req(dto.getReq())
                        .quantity(dto.getQuantity())
                        .amount(dto.getAmount())
                        .build();
                savedOrders.add(ordersRepository.save(orders));
                ProductsOption productsOption = productsOptionRepository.findByProductOptionId(dto.getProductOptionId());
                productsOption.setStock(productsOption.getStock() - dto.getQuantity());
                productsOptionRepository.save(productsOption);
            }
        }

        // 저장된 주문들을 DTO로 변환하여 반환
        return savedOrders.stream()
                .map(this::convertToDT1)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    // 데이터 일관성 보장(address, phoneNumber 둘 다 맞는 값이어야지 저장이 돼야 함)
    // 예외 처리(처리 중 예외가 발생하면 DB를 rollBack 시키기 위해)
    // Transactinal을 사용함
    public void updateUserDeliveryInfo(String email, String address, String phoneNumber) {
        Users users = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        users.setAddress(address);
        users.setPhoneNumber(phoneNumber);

        userRepository.save(users);
    }

    @Transactional(readOnly = true)
    @Override
    public OrdersConfirmDTO confirmOrder(Long orderId, String email) {

        // ** 주의사항 ** 주문한 사람의 토큰값이 아니면 Exception에 걸림.
        Orders orders = ordersRepository.findById(orderId).orElseThrow(() -> new EntityNotFoundException("주문내역을 찾을 수 없습니다."));
        log.info("orders: " + orders);
        log.info("email: " + email);

        // 주문한 사용자와 현재 인증된 사용자가 일치하는지 확인
        if (!orders.getCustomer().getEmail().equals(email)) {
            throw new UserNotFoundException("User not found");
        }

        return new OrdersConfirmDTO(
                orders.getOrderId(),
                orders.getOrderPayId(),
                String.valueOf(orders.getOrderNumber()),
                orders.getCustomer().getName(),
                orders.getCustomer().getAddress(),
                orders.getAmount(),
                orders.getQuantity()
        );
    }

    @Override
    public OrdersListDTO convertToDTO(Orders orders) {
        OrdersListDTO ordersListDTO = new OrdersListDTO();

        ordersListDTO.setReq(orders.getReq());
        ordersListDTO.setProductOptionId(orders.getProductsOption().getProductOptionId());
        ordersListDTO.setOrderNumber(orders.getOrderNumber());
        ordersListDTO.setOrderDate(orders.getCreateAt());
        ordersListDTO.setBrandName(orders.getProductsOption().getProducts().getBrandName());
        ordersListDTO.setStatus(orders.getStatus());
        ordersListDTO.setAmount(orders.getAmount());
        ordersListDTO.setQuantity(orders.getQuantity());

        // GetOneDTO 리스트 생성 및 설정
        List<GetOneDTO> getOneList = new ArrayList<>();
        getOneList.add(createGetOneDTO(orders.getCustomer()));
        ordersListDTO.setGetOne(getOneList);

        return ordersListDTO;
    }

    private GetOneDTO createGetOneDTO(Users users) {
        return new GetOneDTO(users.getPhoneNumber(), users.getAddress(),users.getCustomer().getPoint());
    }

    // 주문번호 orderNumber 랜덤생성
    private Long generateOrderNumber() {
        long random = new Random().nextInt(1000000); // 6자리 랜덤 숫자

        return random;
    }

    // 영문 대소문자, 숫자, 특수문자 -, _, =로 이루어진 6자 이상 64자 이하의 문자열 이어야함.
    // 위 조건에 해당하는 랜덤 orderPayId값 생성
    private String generateOrderPayId(String ord) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_=";
        String specialChars = "-_=";
        StringBuilder stringBuilder = new StringBuilder(specialChars);
        Random random = new Random();
        int length = random.nextInt(59) + 6; // 6 to 64 characters

        for (int i = specialChars.length(); i < length; i++) {
            stringBuilder.append(chars.charAt(random.nextInt(chars.length())));
        }

        List<Character> charList = new ArrayList<>();
        for (char c : stringBuilder.toString().toCharArray()) {
            charList.add(c);
        }
        Collections.shuffle(charList);

        StringBuilder orderPayId = new StringBuilder();
        for (char c : charList) {
            orderPayId.append(c);
        }
        return orderPayId.toString();
    }

    private ProductsOption findProductOption(Long productOptionId) {
        return productsOptionRepository.findById(productOptionId)
                .orElseThrow(() -> new EntityNotFoundException("ProductsOption not found with id: " + productOptionId));
    }


    @Override
    public PageInfinityResponseDTO<OrdersCardListDTO> getOrderCardList(PageRequestDTO pageRequestDTO,String email) {


        Pageable pageable = PageRequest.of(
                pageRequestDTO.getPageIndex() <= 0 ? 0 : pageRequestDTO.getPageIndex() - 1,
                pageRequestDTO.getSize(),
                Sort.by(Sort.Order.desc("createAt")));

        Users users = userRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("User not found"));

        Page<Orders> orders = ordersRepository.findPagingOrdersByCustomer(pageable,users);

        List<OrdersCardListDTO> dtoList = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");


                for(Orders order : orders.getContent()) {
                OrdersCardListDTO dto = new OrdersCardListDTO();
                Review reviews = reviewRepository.findByUsersAndOrders(users,order);
                if(reviews != null) {
                    dto.setReview(true);
                    dto.setReviewId(reviews.getReviewId());
                } else{
                    dto.setReview(false);
                }
                DeliveryTrack deliveryTrack = deliveryTrackRepository.findByOrders(order);
                ProductsImage image = productsImageRepository.findByProductId(order.getProductsOption().getProducts().getProductId());
                dto.setOrderId(order.getOrderId());
                dto.setOrderDate(order.getCreateAt().format(formatter));
                dto.setStatus(order.getStatus());
                dto.setAmount(order.getAmount());
                dto.setQuantity(order.getQuantity());
                dto.setColor(order.getProductsOption().getProducts().getColor());
                dto.setSize(order.getProductsOption().getSize());
                dto.setBrandName(order.getProductsOption().getProducts().getBrandName());
                dto.setProductName(order.getProductsOption().getProducts().getName());
                dto.setProductId(order.getProductsOption().getProducts().getProductId());
                dto.setCustomerName(users.getName());
                dto.setAddress(users.getAddress());
                dto.setProductOptionId(order.getProductsOption().getProductOptionId());
                if (deliveryTrack != null) {
                    dto.setTrackingNumber(deliveryTrack.getTrackingNumber());
                    dto.setCarrierId(deliveryTrack.getCarrierId());
                } else {
                    dto.setTrackingNumber(null);
                    dto.setCarrierId(null);
                }
                dto.setProductImage(image.getUrl_1());
                dtoList.add(dto);
            }

        return PageInfinityResponseDTO.<OrdersCardListDTO>withAll()
                .pageRequestDTO(pageRequestDTO)
                .dtoList(dtoList)
                .totalIndex((int) orders.getTotalElements())
                .build();
    }

    @Override
    public PageInfinityResponseDTO<OrdersCardListDTO> getRefundedOrderCardList(PageRequestDTO pageRequestDTO,String email) {


        Pageable pageable = PageRequest.of(
                pageRequestDTO.getPageIndex() <= 0 ? 0 : pageRequestDTO.getPageIndex() - 1,
                pageRequestDTO.getSize(),
                Sort.by(Sort.Order.desc("createAt")));

        Users users = userRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("User not found"));

        Page<Orders> orders = ordersRepository.findPagingRefundedOrdersByCustomer(pageable,users);

        List<OrdersCardListDTO> dtoList = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");


        for(Orders order : orders.getContent()) {
            OrdersCardListDTO dto = new OrdersCardListDTO();
            Review reviews = reviewRepository.findByUsersAndOrders(users,order);
            if(reviews != null) {
                dto.setReview(true);
                dto.setReviewId(reviews.getReviewId());
            } else{
                dto.setReview(false);
            }
            DeliveryTrack deliveryTrack = deliveryTrackRepository.findByOrders(order);
            ProductsImage image = productsImageRepository.findByProductId(order.getProductsOption().getProducts().getProductId());
            dto.setOrderId(order.getOrderId());
            dto.setOrderDate(order.getCreateAt().format(formatter));
            dto.setStatus(order.getStatus());
            dto.setAmount(order.getAmount());
            dto.setQuantity(order.getQuantity());
            dto.setColor(order.getProductsOption().getProducts().getColor());
            dto.setSize(order.getProductsOption().getSize());
            dto.setBrandName(order.getProductsOption().getProducts().getBrandName());
            dto.setProductName(order.getProductsOption().getProducts().getName());
            dto.setProductId(order.getProductsOption().getProducts().getProductId());
            dto.setCustomerName(users.getName());
            dto.setAddress(users.getAddress());
            dto.setProductOptionId(order.getProductsOption().getProductOptionId());
            if (deliveryTrack != null) {
                dto.setTrackingNumber(deliveryTrack.getTrackingNumber());
                dto.setCarrierId(deliveryTrack.getCarrierId());
            } else {
                dto.setTrackingNumber(null);
                dto.setCarrierId(null);
            }
            dto.setProductImage(image.getUrl_1());
            dtoList.add(dto);
        }

        return PageInfinityResponseDTO.<OrdersCardListDTO>withAll()
                .pageRequestDTO(pageRequestDTO)
                .dtoList(dtoList)
                .totalIndex((int) orders.getTotalElements())
                .build();
    }

    @Override
    public ProductHeaderDTO getProductHeader(Long orderId, String email) {

        Users users = userRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("User not found"));

        log.info(email);
        log.info(orderId);

        log.info(users.getUserId());
        Orders orders = ordersRepository.findByOrderIdAndUserId(orderId,users);
        log.info(orders);
        ProductHeaderDTO dto = new ProductHeaderDTO();
        dto.setProductId(orders.getProductsOption().getProducts().getProductId());
        dto.setName(orders.getProductsOption().getProducts().getName());
        dto.setColor(orders.getProductsOption().getProducts().getColor());

        return dto;
    }

    private OrdersDTO convertToDT1(Orders order) {
        return new OrdersDTO(
                order.getOrderPayId(),
                order.getProductsOption().getProductOptionId(),
                order.getReq(),
                order.getQuantity(),
                order.getAmount(),
                order.getUsePoint(),
                order.getPaymentKey()
        );
    }

    @Override
    public List<OrderInfoByBrandDTO> getOrderListByBrand(String email) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        Users user = userRepository.findByEmail(email).orElse(null);

        List<ProductsBrand> productsBrandList = productsBrandRepository.findByBrand(user.getBrand());
        List<OrderInfoByBrandDTO> list = new ArrayList<>();
        for(ProductsBrand productsBrand : productsBrandList){
            List<Orders> orders = ordersRepository.findByProductId(productsBrand.getProducts().getProductId());
            for(Orders order : orders) {
                OrderInfoByBrandDTO dto = new OrderInfoByBrandDTO();
                dto.setOrderId(order.getOrderId());
                dto.setOrderNumber(order.getOrderNumber());
                dto.setQuantity(order.getQuantity());
                dto.setSize(order.getProductsOption().getSize());
                dto.setColor(order.getProductsOption().getProducts().getColor());
                dto.setStatus(order.getStatus());
                dto.setProductName(order.getProductsOption().getProducts().getName());
                dto.setOrderDate(order.getCreateAt().format(formatter));
                dto.setReq(order.getReq());
                dto.setAmount(order.getAmount());
                dto.setCustomerName(order.getCustomer().getName());
                list.add(dto);
            }
        }

        return list;
    }

    @Override
    public void updateOrderStatusByBrand(OrdersStatusUpdateDTO dto) {
        Orders orders = ordersRepository.findById(dto.getOrderId()).orElse(null);

        assert orders != null;
        orders.setStatus(dto.getStatus());

        ordersRepository.save(orders);

        log.info(orders);
    }

    @Override
    public void cancelOrder(OrderCancelDTO dto) {
        // 주어진 주문 ID로 주문을 조회합니다.
        Orders orders = ordersRepository.findById(dto.getOrderId()).orElse(null);

        // RestTemplate을 생성하여 HTTP 요청을 보냅니다.
        RestTemplate restTemplate = new RestTemplate();

        // 요청 헤더를 설정합니다. (헤더 설정 메서드는 별도로 정의되어야 함)
        HttpHeaders headers = getHeaders();

        // 요청 파라미터를 설정합니다.
        JSONObject params = new JSONObject();
        params.put("cancelReason", dto.getReason()); // 취소 사유
        params.put("cancelAmount", orders.getAmount()); // 취소 금액

        // 결제 취소 API의 URL을 생성합니다.
        String url = "https://api.tosspayments.com/v1/payments/" + orders.getPaymentKey() + "/cancel";

        // 요청 본문과 헤더를 포함한 HttpEntity 객체를 생성합니다.
        HttpEntity<String> requestEntity = new HttpEntity<>(params.toString(), headers);

        try {
            // 결제 취소 API에 POST 요청을 보냅니다.
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            // 응답 상태 코드가 2xx 성공 범위인 경우
            if (response.getStatusCode().is2xxSuccessful()) {
                // 주문 상태를 "결제 취소"로 업데이트합니다.
                orders.setStatus("결제 취소");
                ordersRepository.save(orders); // 변경된 주문 상태를 저장합니다.
                log.info("주문 ID: " + dto.getOrderId() + "의 상태가 결제 취소로 업데이트되었습니다.");
            } else {
                // 결제 취소 실패 시, 응답 코드와 함께 로그를 기록합니다.
                log.error("결제 취소 실패. 응답 코드: " + response.getStatusCode());
            }
        } catch (Exception e) {
            // 예외가 발생한 경우, 오류 메시지와 함께 로그를 기록합니다.
            log.error("결제 취소 중 오류 발생: ", e);
        }
    }


    private HttpHeaders getHeaders(){
        HttpHeaders httpHeaders = new HttpHeaders();
        String testAPIKey = new String(Base64.getEncoder().encode("test_sk_PBal2vxj814Q4P6pa7vkr5RQgOAN:".getBytes(StandardCharsets.UTF_8)));
        httpHeaders.setBasicAuth(testAPIKey);
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        httpHeaders.set("Idempotency-Key", generateIdempotencyKey());
        return httpHeaders;
    }

    private String generateIdempotencyKey() {
        return UUID.randomUUID().toString();
    }

    @Override
    public void refundOrder(OrderCancelDTO dto) {
        Orders orders = ordersRepository.findById(dto.getOrderId()).orElse(null);
        orders.setStatus("환불 요청");
        OrdersRefund ordersRefund = OrdersRefund.builder()
                .order(orders)
                .reason(dto.getReason())
                .build();
        ordersRefundRepository.save(ordersRefund);
        ordersRepository.save(orders);
    }

    @Override
    public void orderComplete(Long orderId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String currentUserName = authentication.getName();
        log.info("이름:"+currentUserName);
        Users users = userRepository.findByEmail(currentUserName)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없음"));

        Orders orders = ordersRepository.findByOrderIdAndUserId(orderId, users);
        orders.setStatus("구매 확정");
        ordersRepository.save(orders);
    }
    public List<OrdersCountDTO> getOrdersCountByPaymentAndRefund() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String currentUserName = authentication.getName();
        log.info("이름:"+currentUserName);
        Users users = userRepository.findByEmail(currentUserName)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없음"));
        long refundCount = ordersRepository.countByStatus("환불 요청", users.getBrand()); // 환불 요청된 상품 수
        long paymentCompleteCount = ordersRepository.countByStatus("결제 완료", users.getBrand());

        List<OrdersCountDTO> ordersCountDTOList = new ArrayList<>();
        OrdersCountDTO ordersCountDTO = new OrdersCountDTO();
        ordersCountDTO.setStatus("refund");
        ordersCountDTO.setCount(refundCount);
        ordersCountDTOList.add(ordersCountDTO);
        ordersCountDTO = new OrdersCountDTO();
        ordersCountDTO.setStatus("paymentComplete");
        ordersCountDTO.setCount(paymentCompleteCount);
        ordersCountDTOList.add(ordersCountDTO);
        return ordersCountDTOList;
    }

    @Override
    public List<OrdersSalesAmountDTO> getBrandSalesAmount(int year) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserName = authentication.getName();
        log.info("이름: " + currentUserName);

        Users users = userRepository.findByEmail(currentUserName)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없음"));

        LocalDateTime startDateTime = LocalDateTime.of(year, Month.JANUARY, 1, 0, 0, 0);
        LocalDateTime endDateTime = LocalDateTime.of(year, Month.DECEMBER, 31, 23, 59, 59);

        List<Orders> orders = ordersRepository.findByBrandAndDateRange(users.getBrand(), startDateTime, endDateTime);

        // 월별 매출 데이터 집계 및 정렬
        return getMonthlySalesAmount(orders);
    }

    public List<OrdersSalesAmountDTO> getMonthlySalesAmount(List<Orders> orders) {
        // 날짜 포맷터를 정의합니다.
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        // 월별로 amount를 집계합니다.
        Map<String, Long> monthlySalesMap = orders.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getCreateAt().format(formatter), // 월별 포맷
                        Collectors.summingLong(Orders::getAmount) // amount 합계
                ));

        // 월별 집계 결과를 DTO 리스트로 변환합니다.
        List<OrdersSalesAmountDTO> ordersSalesAmountDTOList = monthlySalesMap.entrySet().stream()
                .map(entry -> new OrdersSalesAmountDTO(
                        entry.getKey(), // "yyyy-MM"
                        entry.getValue() // 월별 총 매출액
                ))
                .sorted(Comparator.comparing(OrdersSalesAmountDTO::getDate)) // 월 순서로 정렬
                .collect(Collectors.toList());

        return ordersSalesAmountDTOList;
    }

    @Override
    public List<OrdersSalesQuantityDTO> getBrandSalesCount(int year) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserName = authentication.getName();
        log.info("이름: " + currentUserName);

        Users users = userRepository.findByEmail(currentUserName)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없음"));

        LocalDateTime startDateTime = LocalDateTime.of(year, Month.JANUARY, 1, 0, 0, 0);
        LocalDateTime endDateTime = LocalDateTime.of(year, Month.DECEMBER, 31, 23, 59, 59);

        List<Orders> orders = ordersRepository.findByBrandAndDateRange(users.getBrand(), startDateTime, endDateTime);
        return getMonthlySalesCount(orders);
    }

    public List<OrdersSalesQuantityDTO> getMonthlySalesCount(List<Orders> orders) {
        // 날짜 포맷터를 정의합니다.
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        // 월별로 quantity를 집계합니다.
        Map<String, Long> monthlySalesMap = orders.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getCreateAt().format(formatter), // 월별 포맷
                        Collectors.summingLong(Orders::getQuantity) // quantity 합계
                ));

        // 월별 집계 결과를 DTO 리스트로 변환합니다.
        List<OrdersSalesQuantityDTO> ordersSalesAmountDTOList = monthlySalesMap.entrySet().stream()
                .map(entry -> new OrdersSalesQuantityDTO(
                        entry.getKey(), // "yyyy-MM"
                        entry.getValue() // 월별 총 수량
                ))
                .sorted(Comparator.comparing(OrdersSalesQuantityDTO::getDate)) // 월 순서로 정렬
                .collect(Collectors.toList());

        return ordersSalesAmountDTOList;
    }

    @Override
    public List<OrdersTopSellingProductsDTO> getBrandTop10SellingProducts() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserName = authentication.getName();
        log.info("이름: " + currentUserName);

        Users users = userRepository.findByEmail(currentUserName)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없음"));
        Pageable pageable = PageRequest.of(0, 10); // 페이지 인덱스 0, 제한 10

        return  ordersRepository.findTopSellingProducts(users.getBrand(), pageable);
    }


}
