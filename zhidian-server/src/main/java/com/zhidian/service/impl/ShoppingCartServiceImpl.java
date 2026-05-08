package com.zhidian.service.impl;

import cn.hutool.json.JSONUtil;
import com.zhidian.context.BaseContext;
import com.zhidian.dto.ShoppingCartDTO;
import com.zhidian.entity.Dish;
import com.zhidian.entity.Setmeal;
import com.zhidian.entity.ShoppingCart;
import com.zhidian.mapper.DishMapper;
import com.zhidian.mapper.SetmealMapper;
import com.zhidian.mapper.ShoppingCartMapper;
import com.zhidian.service.ShoppingCartService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.zhidian.utils.RedisConstant.SHOPPING_CART_KEY;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private void rebuildCartCache(Long userId) {
        // 注释缓存删除逻辑
        // 缓存删除
        stringRedisTemplate.delete(SHOPPING_CART_KEY + userId);
        try {
            List<ShoppingCart> fullList = shoppingCartMapper.list(
                    ShoppingCart.builder().userId(userId).build()
            );
            // 注释缓存设置
            stringRedisTemplate.opsForValue()
                    .set(SHOPPING_CART_KEY + userId, JSONUtil.toJsonStr(fullList), 30, TimeUnit.MINUTES);
            log.info("购物车缓存同步重建完成，用户ID: {}", userId);
        } catch (Exception e) {
            log.error("同步重建购物车缓存失败，用户ID: {}, 错误信息: {}", userId, e.getMessage(), e);
        }
    }

    @Transactional
    public void addShoppingCart(ShoppingCartDTO dto) {
        Long userId = BaseContext.getCurrentId();
        System.out.println("userId = " + userId);
        // 构造查询条件（只查当前要添加的商品）
        ShoppingCart query = ShoppingCart.builder()
                .userId(userId)
                .dishId(dto.getDishId())
                .setmealId(dto.getSetmealId())
                .build();

        List<ShoppingCart> exists = shoppingCartMapper.list(query);

        if (!exists.isEmpty()) {
            // 已存在：数量+1
            ShoppingCart cart = exists.get(0);
            cart.setNumber(cart.getNumber() + 1);
            shoppingCartMapper.updateNumberById(cart);
        } else {
            // 不存在：新增
            ShoppingCart newCart = new ShoppingCart();
            BeanUtils.copyProperties(dto, newCart);
            newCart.setUserId(userId);
            newCart.setNumber(1);
            newCart.setCreateTime(LocalDateTime.now());

            if (dto.getDishId() != null) {
                Dish dish = dishMapper.getById(dto.getDishId());
                newCart.setName(dish.getName());
                newCart.setImage(dish.getImage());
                newCart.setAmount(dish.getPrice());
            } else {
                Setmeal setmeal = setmealMapper.getById(dto.getSetmealId());
                newCart.setName(setmeal.getName());
                newCart.setImage(setmeal.getImage());
                newCart.setAmount(setmeal.getPrice());
            }
            int rows = shoppingCartMapper.insert(newCart);
            if (rows <= 0) {
                log.info("新增购物车项失败，用户ID: {}, 商品ID: {}", userId, dto.getDishId() != null ? dto.getDishId() : dto.getSetmealId());
            } else {
                log.info("新增购物车项成功，用户ID: {}, 商品ID: {}", userId, dto.getDishId() != null ? dto.getDishId() : dto.getSetmealId());
            }
        }
        stringRedisTemplate.delete(SHOPPING_CART_KEY + userId);
    }

    @Override
    public List<ShoppingCart> showShoppingCart() {
        Long userId = BaseContext.getCurrentId();
        // 注释缓存读取逻辑
         String key = SHOPPING_CART_KEY + userId;

         String json = stringRedisTemplate.opsForValue().get(key);
         if (json != null && !json.isEmpty()) {
             try {
                 return JSONUtil.toList(json, ShoppingCart.class);
             } catch (Exception e) {
                 log.warn("缓存解析失败，key={}", key, e);
             }
         }
        // 直接查询数据库
        List<ShoppingCart> dbList = shoppingCartMapper.list(
                ShoppingCart.builder().userId(userId).build()
        );

         // 注释缓存写入逻辑
         if (dbList != null) {
             stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(dbList), 30, TimeUnit.MINUTES);
         }

        return dbList != null ? dbList : Collections.emptyList();
    }

    @Override
    public void cleanShoppingCart() {
        Long userId = BaseContext.getCurrentId();
        shoppingCartMapper.deleteCartById(userId);
        // 注释缓存删除
         String key = SHOPPING_CART_KEY + userId;
         stringRedisTemplate.delete(key);
    }

    @Override
    public void subShopping(ShoppingCartDTO dto) {
        Long userId = BaseContext.getCurrentId();

        ShoppingCart query = ShoppingCart.builder()
                .userId(userId)
                .dishId(dto.getDishId())
                .setmealId(dto.getSetmealId())
                .build();

        List<ShoppingCart> exists = shoppingCartMapper.list(query);

        if (!exists.isEmpty()) {
            ShoppingCart cart = exists.get(0);
            if (cart.getNumber() > 1) {
                cart.setNumber(cart.getNumber() - 1);
                shoppingCartMapper.updateNumberById(cart);
            } else {
                // 数量为1，直接删除
                shoppingCartMapper.deleteCartOption(cart.getId());
            }
        }
        // 删除缓存
         stringRedisTemplate.delete(SHOPPING_CART_KEY + userId);
    }

    public void addShoppingCartByUserId(ShoppingCartDTO shoppingCartDTO, Long userId) {
        try {
            // 先设置用户 ID 到上下文
            BaseContext.setCurrentId(userId);
            log.info("addShoppingCartByUserId: 已设置 userId={}", userId);
            ShoppingCartService proxy = (ShoppingCartService) AopContext.currentProxy();
            proxy.addShoppingCart(shoppingCartDTO);
        } finally {
            // 清理上下文
            BaseContext.removeCurrentId();
        }
    }

}