package com.daewon.xeno_backend.service;

import com.daewon.xeno_backend.domain.DeliveryTrack;
import com.daewon.xeno_backend.domain.Orders;
import com.daewon.xeno_backend.domain.ProductsImage;
import com.daewon.xeno_backend.repository.DeliveryTrackRepository;
import com.daewon.xeno_backend.repository.OrdersRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Log4j2
public class DeliveryStatusSchedulerService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper jacksonObjectMapper;
    private final OrdersRepository ordersRepository;
    private final DeliveryTrackRepository deliveryTrackRepository;

    public DeliveryStatusSchedulerService(ObjectMapper jacksonObjectMapper, OrdersRepository ordersRepository, DeliveryTrackRepository deliveryTrackRepository) {
        this.jacksonObjectMapper = jacksonObjectMapper;
        this.ordersRepository = ordersRepository;
        this.deliveryTrackRepository = deliveryTrackRepository;
    }

    @Transactional
    @Scheduled(cron = "0 */2 * * * ?") // 매 2분마다 실행
//    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정에 실행
    public void updateDeliveryStatus() {

        List<DeliveryTrack> deliveryTracks = deliveryTrackRepository.findOrdersWithStatusIn();

        for(DeliveryTrack deliveryTrack : deliveryTracks) {
        String url = "https://apis.tracker.delivery/graphql";

        // HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "TRACKQL-API-KEY 97vetlqkkgltulpe7tmfq9pr2:3ii271qrmmir9pvnsj23s34cvv2l3nokhhn9desbbtkb08cqjoi");

        // GraphQL 요청 내용
        String query = "query Track($carrierId: ID!, $trackingNumber: String!) {" +
                "track(carrierId: $carrierId, trackingNumber: $trackingNumber) {" +
                "lastEvent {" +
                "time " +
                "status {" +
                "code " +
                "}" +
                "}" +
                "}" +
                "}";

        // 요청 바디 작성
        Map<String, Object> variables = new HashMap<>();
        variables.put("carrierId", deliveryTrack.getCarrierId());
            // trackingNumber를 문자열에서 숫자로 변환
            String trackingNumberStr = deliveryTrack.getTrackingNumber();
          
                variables.put("trackingNumber", trackingNumberStr);
        Map<String, Object> body = new HashMap<>();
        body.put("query", query);
        body.put("variables", variables);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            // API 요청
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            String responseBody = response.getBody();
            if (responseBody != null) {
                JsonNode jsonNode = jacksonObjectMapper.readTree(responseBody);
                JsonNode errorsNode = jsonNode.path("errors");
                JsonNode dataNode = jsonNode.path("data").path("track");

                // 처리 성공 여부 확인
                if (errorsNode.isArray() && errorsNode.size() > 0) {
                    JsonNode firstError = errorsNode.get(0);
                    String errorMessage = firstError.path("message").asText();
                    log.error("Error message: " + errorMessage);
                } else {
                    JsonNode lastEventNode = dataNode.path("lastEvent");
                    JsonNode statusNode = lastEventNode.path("status");
                    JsonNode timeNode = lastEventNode.path("time");
                    deliveryTrack.setLastEventTime(timeNode.asText());
                    deliveryTrackRepository.save(deliveryTrack);
                    String statusCode = statusNode.path("code").asText();
                    String statusName = statusNode.path("name").asText();
                    log.info("응답 성공"+statusCode);
                    switch (statusCode) {
                        case "UNKNOWN":
                            // 상태가 불확실한 경우, 기본 상태로 설정하거나 로그 기록
                            deliveryTrack.getOrder().setStatus("상태 불명");
                            break;
                        case "INFORMATION_RECEIVED":
                            // 정보 수신 상태일 때의 처리, 예를 들어 초기 상태로 설정
                            deliveryTrack.getOrder().setStatus("정보 수신");
                            break;
                        case "AT_PICKUP":
                            // 픽업 지점에 도착한 경우의 처리
                            deliveryTrack.getOrder().setStatus("픽업 완료");
                            break;
                        case "IN_TRANSIT":
                            // 배송 중인 경우
                            deliveryTrack.getOrder().setStatus("배송 중");
                            break;
                        case "OUT_FOR_DELIVERY":
                            // 배송 중 최종 단계
                            deliveryTrack.getOrder().setStatus("배송 준비 완료");
                            break;
                        case "ATTEMPT_FAIL":
                            // 배송 시도 실패
                            deliveryTrack.getOrder().setStatus("배송 시도 실패");
                            break;
                        case "DELIVERED":
                            // 배송 완료
                            deliveryTrack.getOrder().setStatus("배송 완료");
                            break;
                        case "AVAILABLE_FOR_PICKUP":
                            // 수취를 위해 준비된 상태
                            deliveryTrack.getOrder().setStatus("픽업 가능");
                            break;
                        case "EXCEPTION":
                            // 배송 중 예외 발생
                            deliveryTrack.getOrder().setStatus("배송 예외");
                            break;
                        default:
                            // 예상하지 못한 상태 코드 처리
                            deliveryTrack.getOrder().setStatus("알 수 없는 상태: " + statusCode);
                            break;
                    }

                    ordersRepository.save(deliveryTrack.getOrder());
                }
            } else {
                log.warn("Response body is null.");
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("HTTP error during API request: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error: " + e.getMessage(), e);
        }

    }
    }

}
