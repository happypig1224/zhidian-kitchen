package com.zhidian.mapper;

import com.github.pagehelper.Page;
import com.zhidian.dto.GoodsSalesDTO;
import com.zhidian.dto.OrdersPageQueryDTO;
import com.zhidian.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {
    void insert(Orders order);

    /**
     * 根据订单号查询订单
     *
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     *
     * @param orders
     */
    void update(Orders orders);

    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    @Select("select * from orders where id=#{id}")
    Orders getById(Long id);

    @Select("select * from orders")
    List<Orders> getAll();

    /**
     * 根据条件统计菜品数量
     *
     * @param map
     * @return
     */
    Integer countByMap(Map map);

    @Select("select * from orders where status=#{status} and order_time<#{time}")
    List<Orders> getByStatusAndOrderTimeLT(Integer status, LocalDateTime time);

    Double sumByMap(Map map);

    Integer orderCountByMap(Map map);

    /**
     * 查询商品销量排名
     *
     * @param begin
     * @param end
     */
    List<GoodsSalesDTO> getSalesTop10(LocalDateTime begin, LocalDateTime end);

    /**
     * 乐观锁更新订单
     *
     * @param orderId 订单ID
     */
    @Update("update orders set cancel_reason='支付超时',status=6 ,version=version+1 where id=#{orderId} and status=1")
    int updateOptimize(Long orderId);
}
