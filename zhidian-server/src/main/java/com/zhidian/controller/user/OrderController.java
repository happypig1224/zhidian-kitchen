package com.zhidian.controller.user;

import com.zhidian.dto.OrdersPaymentDTO;
import com.zhidian.dto.OrdersSubmitDTO;
import com.zhidian.result.PageResult;
import com.zhidian.result.Result;
import com.zhidian.service.OrderService;
import com.zhidian.vo.OrderPaymentVO;
import com.zhidian.vo.OrderSubmitVO;
import com.zhidian.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("orderController")
@Tag(name = "订单相关接口")
@Slf4j
@RequestMapping("/user/order")
public class OrderController {
    @Autowired
    private OrderService orderService;

    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @PostMapping("/submit")
    @Operation(summary = "用户下单")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO) {
        log.info("用户下单：{}", ordersSubmitDTO);
        OrderSubmitVO orderSubmitVO = orderService.submitOrder(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }
    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @Operation(summary = "订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        log.info("生成预支付交易单：{}", orderPaymentVO);
        return Result.success(orderPaymentVO);
    }

    @GetMapping("/historyOrders")
    @Operation(summary = "历史订单查询")
    public Result<PageResult> page(int page, int pageSize, Integer status) {
        PageResult pageResult = orderService.pageQueryUser(page, pageSize, status);
        return Result.success(pageResult);
    }

    @GetMapping("orderDetail/{id}")
    @Operation(summary = "订单详情")
    public Result<OrderVO> detailsOrder(@PathVariable Long id){
        OrderVO orderVO=orderService.getDetailByOrderId(id);
        System.out.println("order = " + orderVO);
        return Result.success(orderVO);
    }
    @PutMapping("/cancel/{id}")
    @Operation(summary = "取消订单")
    public Result cancelOrder(@PathVariable Long id){
        orderService.cancelOrder(id);
        return Result.success();
    }

    @PostMapping("/repetition/{id}")
    @Operation(summary = "再来一单")
    public Result repetition(@PathVariable Long id){
        orderService.repetition(id);
        return Result.success();
    }

    /**
     * 用户催单
     *
     * @param id
     * @return
     */
    @GetMapping("/reminder/{id}")
    @Operation(summary = "用户催单")
    public Result reminder(@PathVariable("id") Long id) {
        orderService.reminder(id);
        return Result.success();
    }
}
