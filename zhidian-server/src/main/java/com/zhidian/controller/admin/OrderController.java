package com.zhidian.controller.admin;

import com.zhidian.dto.*;
import com.zhidian.result.PageResult;
import com.zhidian.result.Result;
import com.zhidian.service.OrderService;
import com.zhidian.vo.OrderStatisticsVO;
import com.zhidian.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("adminOrderController")
@Slf4j
@RequestMapping("/admin/order")
@Tag(name = "Admin:订单相关接口")
public class OrderController {
    @Autowired
    private OrderService orderService;

    /**
     * 订单搜索
     * @return
     */
    @GetMapping("/conditionSearch")
    @Operation(summary = "订单搜索")
    public Result<PageResult> conditionSearch(OrdersPageQueryDTO pageQueryDTO){
        PageResult pageResult=orderService.OrdersSearch(pageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 统计订单状态数量
     * @return
     */
    @GetMapping("/statistics")
    @Operation(summary = "各个状态的订单数量统计")
    public Result<OrderStatisticsVO> orderStatistics(){
        OrderStatisticsVO orderStatisticsVO=orderService.getOrderStatistics();
        return Result.success(orderStatisticsVO);
    }
    @GetMapping("/details/{id}")
    @Operation(summary = "订单详情")
    public Result<OrderVO> orderDetail(@PathVariable Long id){
        OrderVO orderVO=orderService.getDetailByOrderId(id);
        return Result.success(orderVO);
    }

    /**
     * 接单
     * @param ordersConfirmDTO
     * @return
     */
    @PutMapping("/confirm")
    @Operation(summary = "接单")
    public Result confirm(@RequestBody OrdersConfirmDTO ordersConfirmDTO){
        orderService.confirm(ordersConfirmDTO);
        return Result.success();
    }

    @PutMapping("/rejection")
    @Operation(summary = "拒单")
    public Result rejection(@RequestBody OrdersRejectionDTO ordersRejectionDTO){
        orderService.rejection(ordersRejectionDTO);
        return Result.success();
    }

    /**
     * 取消订单
     * @param ordersCancelDTO
     * @return
     */
    @PutMapping("/cancel")
    @Operation(summary = "取消订单")
    public Result cancelOrder(@RequestBody OrdersCancelDTO ordersCancelDTO){
        orderService.cancelOrderAdmin(ordersCancelDTO);
        return Result.success();
    }

    @PutMapping("/delivery/{id}")
    @Operation(summary = "派送订单")
    public Result delivery(@PathVariable Long id){
        orderService.delivery(id);
        return Result.success();
    }

    @PutMapping("/complete/{id}")
    @Operation(summary = "完成订单")
    public Result complete(@PathVariable Long id){
        orderService.complete(id);
        return Result.success();
    }
}
