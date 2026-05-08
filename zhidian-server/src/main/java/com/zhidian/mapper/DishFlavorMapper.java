package com.zhidian.mapper;

import com.zhidian.annotation.AutoFill;
import com.zhidian.entity.DishFlavor;
import com.zhidian.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
@Mapper
public interface DishFlavorMapper {
    @AutoFill(OperationType.INSERT)
    void insertBatch(List<DishFlavor> flavors);
    @Select("select * from dish_flavor where dish_id=#{id}")
    List<DishFlavor> getByDishId(Long id);
    @Delete("delete from dish_flavor where dish_id=#{id}")
    void deleteByDishId(Long id);
}
