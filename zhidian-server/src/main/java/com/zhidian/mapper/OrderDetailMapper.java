package com.zhidian.mapper;

import com.zhidian.entity.OrderDetail;
import com.zhidian.vo.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderDetailMapper {
    void insertBatch(@Param("orderDetails") List<OrderDetail> orderDetailList);
    @Select("select * from order_detail where order_id = #{orderId}")
    List<OrderDetail> getByOrderId(Long orderId);

    @Select("select * from orders where id=#{id}")
    OrderVO getDetailById(Integer id);
}
