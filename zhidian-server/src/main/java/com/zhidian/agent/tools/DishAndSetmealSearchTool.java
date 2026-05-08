package com.zhidian.agent.tools;

import com.zhidian.entity.Dish;
import com.zhidian.mapper.DishMapper;
import com.zhidian.mapper.SetmealMapper;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author HappyPig
 * @version 1.0
 * @since 2026/3/31 13:39
 */
@Component
public class DishAndSetmealSearchTool {
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    // 搜索菜品套餐工具
    @Tool(description = "当用户询问有哪些菜品或套餐时，调用该工具搜索菜品或套餐")
    public String searchDishAndSetmealTop200() {
        List<String> dishes = dishMapper.getAllTop100Name();
        List<String> setmeals = setmealMapper.getAllTop100Name();
        return "菜品：" + dishes + "、套餐：" + setmeals;
    }
    @Tool(description = "当用户需要推荐菜品时，根据用户相关信息，如预算、口味、人数、场景等信息，调用该工具推荐菜品和套餐")
    public String recommendDishesAndSetmeals(
            @ToolParam(description = "预算范围，可选参数") String budget,           // 预算范围，如"100 元以下"、"100-200 元"
            @ToolParam(description = "口味偏好，可选参数") String taste,            // 口味偏好，如"辣"、"清淡"、"甜"
            @ToolParam(description = "用餐人数，可选参数") Integer peopleCount,     // 用餐人数
            @ToolParam(description = "场景描述，可选参数") String scene) {          // 场景描述，如"家庭聚餐"、"商务宴请"、"朋友聚会"
            
        StringBuilder result = new StringBuilder();
        result.append("【为您推荐】\n");
        // 1. 根据人数推荐合适的套餐
        if (peopleCount != null && peopleCount > 0) {
            List<com.zhidian.entity.Setmeal> setmeals = setmealMapper.getAllTop100();
            List<com.zhidian.entity.Setmeal> suitableSetmeals = setmeals.stream()
                    .filter(setmeal -> {
                        // 根据人数筛选套餐（假设套餐有适合人数的描述或菜品数量）
                        int dishCount = setmealMapper.getDishItemBySetmealId(setmeal.getId()).size();
                        return dishCount >= peopleCount || dishCount >= 2;
                    })
                    .limit(3)
                    .toList();
                
            if (!suitableSetmeals.isEmpty()) {
                result.append("\n🍱 推荐套餐:\n");
                for (com.zhidian.entity.Setmeal setmeal : suitableSetmeals) {
                    result.append(String.format("  - %s (%.2f 元)\n", 
                            setmeal.getName(), setmeal.getPrice()));
                    if (setmeal.getDescription() != null) {
                        result.append(String.format("    %s\n", setmeal.getDescription()));
                    }
                }
            }
        }
            
        // 2. 根据预算和口味推荐菜品
        List<com.zhidian.entity.Dish> dishes = dishMapper.getAllTop100();
        List<com.zhidian.entity.Dish> recommendedDishes = dishes.stream()
                .filter(dish -> {
                    // 预算过滤
                    if (budget != null && !budget.isEmpty()) {
                        BigDecimal price = dish.getPrice();
                        if (budget.contains("以下") || budget.contains("以内")) {
                            try {
                                String amount = budget.replaceAll("[^0-9]", "");
                                BigDecimal maxBudget = new BigDecimal(amount);
                                if (price.compareTo(maxBudget) > 0) {
                                    return false;
                                }
                            } catch (Exception e) {
                                // 忽略解析错误
                            }
                        } else if (budget.contains("-")) {
                            try {
                                String[] range = budget.split("-");
                                BigDecimal min = new BigDecimal(range[0].replaceAll("[^0-9]", ""));
                                BigDecimal max = new BigDecimal(range[1].replaceAll("[^0-9]", ""));
                                if (price.compareTo(min) < 0 || price.compareTo(max) > 0) {
                                    return false;
                                }
                            } catch (Exception e) {
                                // 忽略解析错误
                            }
                        }
                    }
                        
                    // 口味过滤（简单匹配菜品名称或描述）
                    if (taste != null && !taste.isEmpty()) {
                        String name = dish.getName().toLowerCase();
                        String desc = dish.getDescription() != null ? dish.getDescription().toLowerCase() : "";
                        boolean matchesTaste = name.contains(taste) || desc.contains(taste);
                            
                        // 特殊处理常见口味关键词
                        if ("辣".equals(taste) || "麻辣".equals(taste)) {
                            matchesTaste = name.contains("辣") || name.contains("麻") || 
                                         name.contains("水煮") || name.contains("香辣");
                        } else if ("清淡".equals(taste)) {
                            matchesTaste = name.contains("清") || name.contains("蒸") || 
                                         name.contains("白灼") || !name.contains("辣") && !name.contains("炸");
                        }
                            
                        if (!matchesTaste) {
                            return false;
                        }
                    }
                        
                    return true;
                })
                .sorted((a, b) -> b.getSalesVolume() - a.getSalesVolume()) // 按销量排序
                .limit(5)
                .toList();
            
        if (!recommendedDishes.isEmpty()) {
            result.append("\n🥗 推荐菜品:\n");
            for (com.zhidian.entity.Dish dish : recommendedDishes) {
                result.append(String.format("  - %s (%.2f 元)\n", 
                        dish.getName(), dish.getPrice()));
                if (dish.getDescription() != null) {
                    result.append(String.format("    %s\n", dish.getDescription()));
                }
            }
        }
            
        // 3. 如果没有推荐结果，返回默认热销
        if (recommendedDishes.isEmpty() && (peopleCount == null || peopleCount <= 0)) {
            result.append("\n根据您的要求，为您推荐热销菜品:\n");
            dishes.stream()
                    .limit(5)
                    .forEach(dish -> 
                        result.append(String.format("  - %s (%.2f 元)\n", 
                                dish.getName(), dish.getPrice()))
                    );
        }
            
        return result.toString();
    }

}
