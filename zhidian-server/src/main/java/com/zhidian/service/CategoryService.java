package com.zhidian.service;

import com.zhidian.dto.CategoryDTO;
import com.zhidian.dto.CategoryPageQueryDTO;
import com.zhidian.entity.Category;
import com.zhidian.result.PageResult;

import java.util.List;

public interface CategoryService {
    PageResult page(CategoryPageQueryDTO categoryPageQueryDTO);

    void save(CategoryDTO categoryDTO);

    CategoryDTO getById(Long id);

    void update(CategoryDTO categoryDTO);

    void deleteById(Long id);

    void startOrStop(Integer status, Long id);

    List<CategoryDTO> selectAll();

    List<Category> list(Integer type);
}
