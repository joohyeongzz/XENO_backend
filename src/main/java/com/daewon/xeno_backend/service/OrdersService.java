package com.daewon.xeno_backend.service;

import com.daewon.xeno_backend.domain.Orders;
import com.daewon.xeno_backend.dto.order.*;
import com.daewon.xeno_backend.dto.page.PageInfinityResponseDTO;
import com.daewon.xeno_backend.dto.page.PageRequestDTO;
import com.daewon.xeno_backend.dto.product.ProductHeaderDTO;

import java.util.List;
import java.util.Map;

public interface OrdersService {


    void orderComplete(Long orderId);

    void cancelOrder(OrderCancelDTO dto);

    void refundOrder(OrderCancelDTO dto);

    List<OrdersListDTO> getAllOrders(Long userId);

    List<OrdersDTO> createOrders(List<OrdersDTO> ordersDTO, String email);

    void updateUserDeliveryInfo(String email, String address, String phoneNumber);

    OrdersConfirmDTO confirmOrder(Long orderId, String email);

    OrdersListDTO convertToDTO(Orders orders);

    PageInfinityResponseDTO<OrdersCardListDTO> getOrderCardList(PageRequestDTO pageRequestDTO,String email);

    PageInfinityResponseDTO<OrdersCardListDTO> getRefundedOrderCardList(PageRequestDTO pageRequestDTO,String email);

    ProductHeaderDTO getProductHeader(Long orderId, String email);

   List<OrderInfoByBrandDTO> getOrderListByBrand(String email);

    void updateOrderStatusByBrand(OrdersStatusUpdateDTO dto);

    String getLatestReqForUser(String email);

    OrderDeliveryInfoReadDTO getOrderDeliveryInfo(Long userId);
}
