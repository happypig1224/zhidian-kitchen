package com.zhidian.mapper;

import com.github.pagehelper.Page;
import com.zhidian.annotation.AutoFill;
import com.zhidian.dto.SetmealPageQueryDTO;
import com.zhidian.entity.Setmeal;
import com.zhidian.enumeration.OperationType;
import com.zhidian.vo.DishItemVO;
import com.zhidian.vo.SetmealVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SetmealMapper {

    /**
     * 根据分类id查询套餐的数量
     * @param id
     * @return
     */
    @Select("select count(id) from setmeal where category_id = #{categoryId}")
    Integer countByCategoryId(Long id);

    List<Long> getSetmealIdsByDishIds(List<Long> dishIds);
    @Delete("delete from dish_flavor where dish_id=#{dishId}")
    void deleteById(Long dishId);

    /**
     * 新增套餐
     * @param setmeal
     */
    @AutoFill(OperationType.INSERT)
    void insert(Setmeal setmeal);

    Page<SetmealVO> pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    @AutoFill(OperationType.UPDATE)
    void update(Setmeal setmeal);

    @Select("select * from setmeal where id=#{id}")
    Setmeal getById(Long id);

    @Delete("delete from setmeal where id=#{setmealId}")
    void deleteSetmealById(Long setmealId);
    /**
     * 动态条件查询套餐
     * @param setmeal
     * @return
     */
    List<Setmeal> list(Setmeal setmeal);
    // 获取前100条所有套餐，按销量降序排序
    @Select("select * from setmeal order by sales_volume desc limit 100")
    List<Setmeal> getAllTop100();
    /**
     * 根据套餐id查询菜品选项
     * @param setmealId
     * @return
     */
    @Select("select sd.name, sd.copies, d.image, d.description " +
            "from setmeal_dish sd left join dish d on sd.dish_id = d.id " +
            "where sd.setmeal_id = #{setmealId}")
    List<DishItemVO> getDishItemBySetmealId(Long setmealId);

    /**
     * 根据条件统计套餐数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);

    /**
     * 获取最后插入的套餐记录
     * @return
     */
    @Select("select * from setmeal order by id desc limit 1")
    Setmeal getLastInsert();


    @Select("select id from setmeal where name = #{setmealName}")
    Long getByName(String setmealName);

    @Select("select name from setmeal order by sales_volume desc limit 100")
    List<String> getAllTop100Name();

}