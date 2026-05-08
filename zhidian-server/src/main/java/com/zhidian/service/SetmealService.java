package com.zhidian.service;

import com.zhidian.dto.SetmealDTO;
import com.zhidian.dto.SetmealPageQueryDTO;
import com.zhidian.entity.Setmeal;
import com.zhidian.result.PageResult;
import com.zhidian.result.Result;
import com.zhidian.vo.DishItemVO;
import com.zhidian.vo.SetmealVO;

import java.util.List;

public interface SetmealService {
    /**
     * 新增套餐，同时需要保存套餐和菜品的关联关系
     * @param setmealDTO
     */
    Result<String> saveWithDish(SetmealDTO setmealDTO);


    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    Result<String> update(SetmealDTO setmealDTO);

    SetmealVO getById(Long id);

    void startorstop(Integer status, Long id);

    Result<String> delete(List<Long> ids);

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    List<Setmeal> list(Setmeal setmeal);

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    List<DishItemVO> getDishItemById(Long id);

}
