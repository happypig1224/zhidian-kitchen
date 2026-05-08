package com.zhidian.mapper;

import com.github.pagehelper.Page;
import com.zhidian.annotation.AutoFill;
import com.zhidian.dto.CategoryDTO;
import com.zhidian.dto.CategoryPageQueryDTO;
import com.zhidian.entity.Category;
import com.zhidian.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CategoryMapper {
    Page<Category> page(CategoryPageQueryDTO categoryPageQueryDTO);

    @AutoFill(OperationType.INSERT)
    void save(Category category);

    @Select("select * from category where id=#{id}")
    Category getById(Long id);

    @AutoFill(OperationType.UPDATE)
    void update(Category category);

    @Delete("delete from category where id=#{id}")
    void deleteById(Long id);

    @Select("select * from category")
    List<CategoryDTO> findAll();

    List<Category> list(Integer type);
}
