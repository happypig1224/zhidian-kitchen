package com.zhidian.mapper;

import com.zhidian.entity.SeckillOrder;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author 33046
* @description 针对表【seckill_order(秒杀订单表)】的数据库操作Mapper
* @createDate 2026-03-11 16:54:18
* @Entity com.zhidian.entity.SeckillOrder
*/
@Mapper
public interface SeckillOrderMapper {

    /**
     * 保存秒杀订单
     * @param seckillOrder
     */
    @Insert("insert into seckill_order(user_id,voucher_id,order_time,status,create_time) values(#{userId},#{voucherId},#{orderTime},#{status},#{createTime})")
    int save(SeckillOrder seckillOrder);

    /**
     * 查询用户秒杀订单列表
     * @param userId
     * @return
     */
    @Select("select * from seckill_order where user_id = #{userId}")
    List<SeckillOrder> selectList(Long userId);
}




