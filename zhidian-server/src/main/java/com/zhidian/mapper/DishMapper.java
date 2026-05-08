package com.zhidian.mapper;

import com.github.pagehelper.Page;
import com.zhidian.annotation.AutoFill;
import com.zhidian.dto.DishPageQueryDTO;
import com.zhidian.entity.Dish;
import com.zhidian.enumeration.OperationType;
import com.zhidian.vo.DishVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DishMapper {

    /**
     * 根据分类id查询菜品数量
     *
     * @param categoryId
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    @AutoFill(OperationType.INSERT)
    void insert(Dish dish);

    Page<DishVO> queryPage(DishPageQueryDTO dishPageQueryDTO);

    @Delete("delete from dish where id=#{dishId}")
    void deleteByIds(Long dishId);

    @Select("select * from dish where id=#{id}")
    Dish getById(Long id);

    @AutoFill(OperationType.UPDATE)
    void updateDish(Dish dish);

    List<Dish> list(Dish dish);

    @Select("select a.* from dish a left join setmeal_dish b on a.id = b.dish_id where b.setmeal_id = #{setmealId}")
    List<Dish> getBySetmealId(Long id);

    Integer countByMap(Map map);

    // 获取前100条菜品，按销量降序排序
    @Select("select * from dish order by sales_volume desc limit 100")
    List<Dish> getAllTop100();

    @Select("select name from dish order by sales_volume desc limit 500")
    List<String> getTop500DishNames();

    @Select("select id from dish where name=#{dishName}")
    Long getByName(String dishName);

    @Select("select name from dish order by sales_volume desc limit 100")
    List<String> getAllTop100Name();
}
