package com.zhidian.service;

import com.zhidian.dto.DishDTO;
import com.zhidian.dto.DishPageQueryDTO;
import com.zhidian.entity.Dish;
import com.zhidian.result.PageResult;
import com.zhidian.result.Result;
import com.zhidian.vo.DishVO;

import java.util.List;

public interface DishService {

    /**
     * 新增菜品和对应的口味
     *
     * @param dishDTO
     */
    public Result<String> saveWithFlavor(DishDTO dishDTO);

    PageResult queryPage(DishPageQueryDTO dishPageQueryDTO);

    Result<String> deleteBatch(List<Long> ids);

    DishVO getById(Long id);

    Result<String> updateDish(DishDTO dishDTO);

    List<Dish> list(Long categoryId);

    void startOrStop(Integer status, Long id);
    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    List<DishVO> listWithFlavor(Dish dish);

}