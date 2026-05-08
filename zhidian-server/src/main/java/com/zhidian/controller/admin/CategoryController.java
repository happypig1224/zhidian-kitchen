package com.zhidian.controller.admin;

import com.zhidian.dto.CategoryDTO;
import com.zhidian.dto.CategoryPageQueryDTO;
import com.zhidian.result.PageResult;
import com.zhidian.result.Result;
import com.zhidian.service.CategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类管理
 */
@RestController
@RequestMapping("/admin/category")
@Api(tags = "分类相关接口")
@Slf4j
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    @GetMapping("/page")
    @ApiOperation("分页查询分类")
    public Result<PageResult> page(CategoryPageQueryDTO categoryPageQueryDTO){
        log.info("分类分页查询{}",categoryPageQueryDTO);
        PageResult pageResult= categoryService.page(categoryPageQueryDTO);
        return Result.success(pageResult);
    }

    @PostMapping
    @ApiOperation("新增菜品分类")
    public Result save(@RequestBody CategoryDTO categoryDTO){
        log.info("新增分类:{}",categoryDTO
        );
        categoryService.save(categoryDTO);
        return Result.success();
    }
    @PutMapping
    @ApiOperation("修改分类")
    public Result update(@RequestBody CategoryDTO categoryDTO){
        categoryService.update(categoryDTO);
        return Result.success();
    }

    @DeleteMapping
    @ApiOperation("删除分类")
    public Result delete(Long id){
        categoryService.deleteById(id);
        return Result.success();
    }

    @PostMapping("/status/{status}")
    @ApiOperation("禁用或启用分类")
    public Result startOrStop(@PathVariable Integer status,Long id){
        categoryService.startOrStop(status,id);
        return Result.success();
    }

    @GetMapping("/list")
    @ApiOperation("查询分类列表")
    public Result<List<CategoryDTO>> list(Integer type){
        List<CategoryDTO> categoryDTOS= categoryService.selectAll();
        return Result.success(categoryDTOS);
    }
}
