package com.zhidian.agent.tools;

import com.zhidian.context.BaseContext;
import com.zhidian.dto.ShoppingCartDTO;
import com.zhidian.entity.Dish;
import com.zhidian.entity.ShoppingCart;
import com.zhidian.mapper.DishMapper;
import com.zhidian.mapper.SetmealMapper;
import com.zhidian.mapper.ShoppingCartMapper;
import com.zhidian.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author HappyPig
 * @version 1.0
 * @since 2026/3/31 13:40
 */
@Component
@Slf4j
public class AddShoppingCartTool {
    @Autowired
    private ShoppingCartService shoppingCartService;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    @Tool(description = "根据用户需要添加的菜品名称，添加菜品到购物车")
    public String addDishShoppingCart(@ToolParam(description = "菜品名称") String dishName,
                                      @ToolParam(description = "口味选择") String flavors,
                                      ToolContext toolContext) {
        Long userId = (Long) toolContext.getContext().get("userId");
        if (userId == null) {
            log.error("添加购物车失败：用户未登录或身份识别失败");
            return "很抱歉，系统出现异常，暂时无法为您添加商品。请稍后重试，或直接告诉我您想购买的其他菜品。";
        }

        log.info("用户{}添加菜品到购物车，菜名={},口味={}", userId, dishName, flavors);
        try {
            Long dishId = dishMapper.getByName(dishName);
            if (dishId == null) {
                log.error("添加购物车失败：菜品不存在，菜名={}", dishName);
                return "非常抱歉，您说的这道菜目前 unavailable(已售罄或下架)。我为您推荐其他相似菜品，或者您可以告诉我想尝试的其他菜式？";
            }
            ShoppingCartDTO shoppingCartDTO = ShoppingCartDTO.builder()
                    .dishId(dishId)
                    .dishFlavor(flavors)
                    .build();
            shoppingCartService.addShoppingCartByUserId(shoppingCartDTO, userId);
            return "已成功加入购物车";
        } catch (Exception e) {
            log.error("添加购物车异常，用户 ID: {}, 菜品：{}, 错误：{}", userId, dishName, e.getMessage(), e);
            // 不要暴露技术细节给用户
            return "很抱歉，操作未能成功完成。您可以稍后重试，或告诉我还想了解哪些菜品？";
        }
    }
    @Tool(description = "根据用户需要添加的套餐名称，添加套餐到购物车")
    public String addSetmealShoppingCart(@ToolParam(description = "套餐名称") String setmealName,
                                         @ToolParam(description = "口味选择") String flavors,
                                         ToolContext toolContext) {
        // 从 ToolContext 中获取 userId（由 Controller 通过 toolContext(Map.of("userId", userId)) 传递）
        Long userId = (Long) toolContext.getContext().get("userId");
        if (userId == null) {
            log.error("添加购物车失败：用户未登录或身份识别失败");
            return "很抱歉，系统出现异常，暂时无法为您添加商品。请稍后重试，或直接告诉我您想购买的其他套餐。";
        }

        log.info("用户{}添加套餐到购物车，套餐名={}", userId, setmealName);
        try {
            Long setmealId = setmealMapper.getByName(setmealName);
            if (setmealId == null) {
                log.error("添加购物车失败：套餐不存在，套餐名={}", setmealName);
                return "非常抱歉，这个套餐目前 unavailable。我为您推荐其他热门套餐，或者您想了解哪些菜品？";
            }
            
            ShoppingCartDTO shoppingCartDTO = ShoppingCartDTO.builder()
                    .setmealId(setmealId)
                    .dishFlavor(flavors)
                    .build();
            shoppingCartService.addShoppingCartByUserId(shoppingCartDTO, userId);
            return "已成功加入购物车";
        } catch (Exception e) {
            log.error("添加购物车异常，用户 ID: {}, 套餐：{}, 错误：{}", userId, setmealName, e.getMessage(), e);
            // 不要暴露技术细节给用户
            return "很抱歉，操作未能成功完成。您可以稍后重试，或告诉我还想了解哪些菜品或套餐？";
        }
    }
}
