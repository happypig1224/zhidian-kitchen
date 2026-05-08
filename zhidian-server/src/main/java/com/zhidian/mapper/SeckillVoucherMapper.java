package com.zhidian.mapper;

import com.zhidian.entity.SeckillVoucher;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
* @author 33046
* @description 针对表【seckill_voucher(秒杀优惠券表)】的数据库操作Mapper
* @createDate 2026-03-11 16:54:18
* @Entity com.zhidian.entity.SeckillVoucher
*/
@Mapper
public interface SeckillVoucherMapper {
    int issueVoucher(SeckillVoucher seckillVoucher);
    @Update("UPDATE seckill_voucher SET stock = stock - 1 WHERE id = #{voucherId} AND stock > 0")
    int decreaseStock(Long voucherId);

    @Select("SELECT * FROM seckill_voucher")
    List<SeckillVoucher> queryVoucher();
}




