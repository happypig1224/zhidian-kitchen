package com.zhidian.service;

import com.zhidian.dto.*;
import com.zhidian.result.PageResult;
import com.zhidian.vo.OrderPaymentVO;
import com.zhidian.vo.OrderStatisticsVO;
import com.zhidian.vo.OrderSubmitVO;
import com.zhidian.vo.OrderVO;

public interface OrderService {
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);
    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    PageResult pageQueryUser(int page, int pageSize, Integer status);

    OrderVO getDetailByOrderId(Long id);


    void cancelOrder(Long id);

    void repetition(Long id);

    PageResult OrdersSearch(OrdersPageQueryDTO pageQueryDTO);

    OrderStatisticsVO getOrderStatistics();

    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    void rejection(OrdersRejectionDTO ordersRejectionDTO);

    void cancelOrderAdmin(OrdersCancelDTO ordersCancelDTO);

    void delivery(Long id);

    void complete(Long id);

    void reminder(Long id);
}
