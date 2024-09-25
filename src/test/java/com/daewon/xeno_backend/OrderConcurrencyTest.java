package com.daewon.xeno_backend;

import com.daewon.xeno_backend.domain.*;
import com.daewon.xeno_backend.domain.auth.Users;
import com.daewon.xeno_backend.dto.order.OrdersDTO;
import com.daewon.xeno_backend.dto.product.ProductColorInfoCardDTO;
import com.daewon.xeno_backend.dto.product.ProductInfoDTO;
import com.daewon.xeno_backend.repository.LikeRepository;
import com.daewon.xeno_backend.repository.Products.*;
import com.daewon.xeno_backend.repository.ReviewRepository;
import com.daewon.xeno_backend.repository.auth.UserRepository;
import com.daewon.xeno_backend.service.OrdersService;
import com.daewon.xeno_backend.service.ProductService;
import com.daewon.xeno_backend.service.ProductServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class OrderConcurrencyTest {

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private ProductsOptionRepository productsOptionRepository;
    @Autowired
    private ProductsRepository productsRepository;
    @Autowired
    private ProductServiceImpl productServiceImpl;

    @Autowired
    private ProductService productService; // 서비스 클래스

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LikeRepository likeRepository;
    @Autowired
    private ProductsStarRepository productsStarRepository;
    @Autowired
    private ProductsLikeRepository productsLikeRepository;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private ProductsImageRepository productsImageRepository;
    @Autowired
    private ConcurrentMapCacheManager cacheManager;

    @Test
    @Transactional
    public void testConcurrentOrders() throws InterruptedException {
        // 주문 DTO 리스트 생성 (스레드당 3개씩 주문)
        List<OrdersDTO> orderList = new ArrayList<>();

            OrdersDTO ordersDTO = new OrdersDTO();
            ordersDTO.setProductOptionId(1L); // 제품 ID 설정
            ordersDTO.setQuantity(3); // 하나씩 주문
            ordersDTO.setOrderPayId("PAY_ID"); // 임의 값
            ordersDTO.setPaymentKey("PAYMENT_KEY"); // 임의 값
            orderList.add(ordersDTO);


        // 10개의 스레드를 사용하여 동시에 3개씩 주문
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        for (int i = 0; i < 10; i++) {
            executorService.submit(() -> {
                String threadName = Thread.currentThread().getName();
                try {
                    List<OrdersDTO> orders = ordersService.createOrders(orderList, "joohyeongzz@naver.com");
                    System.out.println(orders);
                    ProductsOption updatedProductOption = productsOptionRepository.findByProductOptionId(1L);
                    System.out.println(threadName+"의 업데이트 후 재고 : " +updatedProductOption.getStock());
                } catch (Exception e) {
                    System.out.println(threadName+"예외 발생: " + e.getMessage());
                }
            });
        }

        // 스레드 종료 대기
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);

        // 재고 확인 (29 - 10*3 = 2)
        ProductsOption updatedProductOption = productsOptionRepository.findByProductOptionId(1L);
        System.out.println("최종 재고 : " +updatedProductOption.getStock());
    }

    @Test
    public void testGetProductsInfoByCategory_Original() {
        long startTime = System.currentTimeMillis();

        // 원본 메서드 호출
        List<ProductColorInfoCardDTO> result = productServiceImpl.getProductsInfoByCategory("000", "");

        System.out.println(result);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("Original method duration: " + duration + " ms");

        // 결과 검증 (필요한 경우)
        assertNotNull(result);
    }



    @Test
    public void testGetProductInfoPerformance() throws IOException {

        long startTime = System.currentTimeMillis();

        ProductInfoDTO productInfoDTO = productService.getProductInfo(2L);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;



        assertNotNull(productInfoDTO);


        System.out.println("Average Performance Test Duration: " + duration + " ms");
    }

    @Test
    public void testCaching() throws IOException {
        long startTime = System.currentTimeMillis();

        // 캐시 미스 확인
        productService.getProductInfo(2L);

        long midTime = System.currentTimeMillis();

        // 캐시 히트 확인
        productService.getProductInfo(2L);

        long endTime = System.currentTimeMillis();

        System.out.println("Time with cache miss: " + (midTime - startTime) + " ms");
        System.out.println("Time with cache hit: " + (endTime - midTime) + " ms");
    }

    @Test
    public void testGetProductInfoPerformance2() throws IOException {

            long startTime = System.currentTimeMillis();

            ProductInfoDTO productInfoDTO = productService.getProductInfoTest(2L);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;


            assertNotNull(productInfoDTO);


        System.out.println("Average Performance Test Duration: " + duration + " ms");
    }

    @Test
    public void testGetProductInfoPerformance3() throws IOException {
        int numTests = 100;
        long totalDuration = 0;

        for (int i = 0; i < numTests; i++) {
            long startTime = System.currentTimeMillis();

            List<ProductColorInfoCardDTO> productInfoDTO = productService.getProductsInfoByCategoryOriginal("000","");

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            totalDuration += duration;

            System.out.println("Test " + (i + 1) + " Duration: " + duration + " ms");

            assertNotNull(productInfoDTO);
        }

        double averageDuration = (double) totalDuration / numTests;
        System.out.println("Average Performance Test Duration: " + averageDuration + " ms");
    }

    @Transactional
    @Test
    public void testCacheEffectInDifferentTransactions() throws IOException {
        // 1차 트랜잭션 내에서의 테스트
        ProductInfoDTO productInfo1 = productService.getProductInfo(1L);
        ProductInfoDTO productInfo2 = productService.getProductInfo(1L);

        // 1차 캐시 테스트
        assertEquals(productInfo1, productInfo2);

        // 트랜잭션 종료 후 캐시 비우기
        // 새로운 트랜잭션 시작
        ProductInfoDTO productInfo3 = productService.getProductInfo(1L);

        // 1차 캐시가 비워지고 새로 쿼리된 결과 확인
        assertNotEquals(productInfo1, productInfo3);
    }

    @Test
    public void testCacheWithMultipleThreads() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<ProductInfoDTO> future1 = executor.submit(() -> productService.getProductInfo(2L));
        Future<ProductInfoDTO> future2 = executor.submit(() -> productService.getProductInfo(2L));

        ProductInfoDTO result1 = future1.get();
        ProductInfoDTO result2 = future2.get();

        // 캐시 히트 여부 확인
        assertEquals(result1, result2);

        executor.shutdown();
    }

    @Test
    public void testGetProductInfoPerformanceS() throws ExecutionException, InterruptedException {
        Long productId = 2L; // 테스트할 상품 ID

        // ExecutorService를 통해 A와 B 사용자의 요청을 동시에 실행
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        Callable<Long> callableA = () -> {
            long start = System.currentTimeMillis();
            productService.getProductInfo(productId);
            long end = System.currentTimeMillis();
            return end - start; // 요청 처리 시간 반환
        };

        Callable<Long> callableB = () -> {
            Thread.sleep(1000);
            long start = System.currentTimeMillis();
            productService.getProductInfo(productId);
            long end = System.currentTimeMillis();
            return end - start; // 요청 처리 시간 반환
        };

        Future<Long> futureA = executorService.submit(callableA);
        Future<Long> futureB = executorService.submit(callableB);

        long timeA = futureA.get();
        long timeB = futureB.get();

        executorService.shutdown();

        // 시간 출력
        System.out.println("Time taken for user A: " + timeA + " ms");
        System.out.println("Time taken for user B: " + timeB + " ms");
        Cache cache = cacheManager.getCache("products");
        // 캐시가 비어있지 않은지 확인
        System.out.println("cache: " + cache + " ");
        System.out.println("get: " + cache.get(2L) + " ");


        // Assertion (시간이 너무 길 경우 실패로 처리)
        assertTrue(timeA < 5000, "User A took too long to process");
        assertTrue(timeB < 5000, "User B took too long to process");
    }

    @Test
    public void testCacheBehaviorWithoutFirstLevelCache() throws InterruptedException, IOException {
        Long productId = 2L; // 테스트할 상품 ID

        // 첫 번째 호출
        ProductInfoDTO result1 = productService.getProductInfo(productId);
        System.out.println("First call: " + result1);

        // 캐시 상태 확인
        Cache cache = cacheManager.getCache("products");
        System.out.println("Cache after first call: " + cache.get(productId));

        // 대기 시간 설정
        Thread.sleep(5000);

        // 두 번째 호출
        ProductInfoDTO result2 = productService.getProductInfo(productId);
        System.out.println("Second call: " + result2);

        // 캐시 상태 확인
        System.out.println("Cache after second call: " + cache.get(productId));
    }
}
