package com.zhidian.service.impl;

import cn.hutool.json.JSONUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zhidian.constant.StatusConstant;
import com.zhidian.context.BaseContext;
import com.zhidian.dto.DishDTO;
import com.zhidian.dto.DishPageQueryDTO;
import com.zhidian.entity.Dish;
import com.zhidian.entity.DishFlavor;
import com.zhidian.mapper.DishFlavorMapper;
import com.zhidian.mapper.DishMapper;
import com.zhidian.mapper.SetmealMapper;
import com.zhidian.result.PageResult;
import com.zhidian.result.Result;
import com.zhidian.service.DishService;
import com.zhidian.utils.MemoryCacheUtil;
import com.zhidian.utils.RedisConstant;
import com.zhidian.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private MemoryCacheUtil localCache;

    /**
     * 新增菜品和对应口味
     *
     * @param dishDTO
     */

    public Result<String> saveWithFlavor(DishDTO dishDTO) {
        Result<String> result = transactionTemplate.execute(status -> {
            try {
                log.info("编程式事务开启，新增菜品名称{}", dishDTO.getName());
                // 1. 插入菜品基本信息
                Dish dish = new Dish();
                BeanUtils.copyProperties(dishDTO, dish);
                dish.setCreateTime(LocalDateTime.now());
                dish.setUpdateTime(LocalDateTime.now());
                dish.setCreateUser(BaseContext.getCurrentId());
                dish.setUpdateUser(BaseContext.getCurrentId());
                dishMapper.insert(dish);
                Long dishId = dish.getId();
                // 2.批量插入菜品口味
                List<DishFlavor> flavors = dishDTO.getFlavors();
                if (flavors != null && !flavors.isEmpty()) {
                    flavors.forEach(flavor -> {
                        flavor.setDishId(dishId);
                    });
                    dishFlavorMapper.insertBatch(flavors);
                }
                log.info("添加菜品{}成功", dishDTO.getName());
                return Result.success("添加菜品成功!");
            } catch (BeansException e) {
                log.error("新增菜品和对应口味失败", e);
                status.setRollbackOnly();
                return Result.error("添加失败");
            }
        });
        dishDTO.setStatus(StatusConstant.ENABLE);
        if (result.getCode() == 1) {
            String cacheKey = RedisConstant.DISH_KEY + dishDTO.getCategoryId() + dishDTO.getStatus();
            stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(dishDTO), RedisConstant.CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            localCache.put(cacheKey, dishDTO, RedisConstant.CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        }
        return result;
    }

    @Override
    public PageResult queryPage(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> dishVOPage = dishMapper.queryPage(dishPageQueryDTO);
        return new PageResult(dishVOPage.getTotal(), dishVOPage.getResult());
    }

    public Result<String> deleteBatch(List<Long> ids) {
        List<Long> successIds = new ArrayList<>();
        List<Long> failedIds = new ArrayList<>();
        Set<Long> categoryIds = new HashSet<>(); // 记录需要失效缓存的分类ID

        // 1. 前置校验
        for (Long id : ids) {
            try {
                Dish dish = dishMapper.getById(id);
                if (dish == null) {
                    failedIds.add(id);
                    continue;
                }
                if (dish.getStatus().equals(StatusConstant.ENABLE)) {
                    failedIds.add(id);
                    continue;
                }
                // 记录分类ID用于后续缓存失效
                categoryIds.add(dish.getCategoryId());
            } catch (Exception e) {
                failedIds.add(id);
                log.warn("菜品ID: {} 校验失败", id, e);
            }
        }

        // 2. 检查关联套餐
        try {
            List<Long> setmealIdsByDishIds = setmealMapper.getSetmealIdsByDishIds(ids);
            if (setmealIdsByDishIds != null && !setmealIdsByDishIds.isEmpty()) {
                return Result.error("菜品中包含关联套餐，无法删除!");
            }
        } catch (Exception e) {
            return Result.error("关联套餐检查失败: " + e.getMessage());
        }

        // 3. 逐个菜品处理事务
        Result<String> singleResult = null;
        for (Long id : ids) {
            if (failedIds.contains(id)) {
                continue; // 跳过已失败的菜品
            }

            singleResult = transactionTemplate.execute(status -> {
                try {
                    dishMapper.deleteByIds(id);
                    dishFlavorMapper.deleteByDishId(id);
                    successIds.add(id);
                    return Result.success("删除成功");
                } catch (Exception e) {
                    status.setRollbackOnly();
                    failedIds.add(id);
                    log.error("菜品ID: {} 删除失败", id, e);
                    return Result.error("删除失败");
                }
            });
        }

        // 4. 事务成功后，执行Redis缓存失效操作
        if (!successIds.isEmpty() && singleResult.getCode() == 1) {
            for (Long categoryId : categoryIds) {
                try {
                    String cacheKey = RedisConstant.DISH_KEY + categoryId + ":*";
                    stringRedisTemplate.delete(cacheKey);
                    cacheKey = RedisConstant.DISH_KEY + categoryId + ":" + StatusConstant.ENABLE;
                    localCache.delete(cacheKey);
                    log.info("批量菜品删除成功，失效分类ID: {} 的相关缓存", categoryId);
                } catch (Exception e) {
                    log.warn("缓存失效操作失败，分类ID: {}", categoryId, e);
                }
            }
        }

        // 5. 返回详细结果
        if (failedIds.isEmpty()) {
            return Result.success("批量删除成功，共删除 " + successIds.size() + " 个菜品");
        } else if (successIds.isEmpty()) {
            return Result.error("批量删除失败，所有菜品删除失败");
        } else {
            return Result.success("部分删除成功，成功: " + successIds.size() + " 个，失败: " + failedIds.size() + " 个");
        }
    }

    @Override
    public DishVO getById(Long id) {
        Dish dish = dishMapper.getById(id);
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    public Result<String> updateDish(DishDTO dishDTO) {
        Result<String> result = transactionTemplate.execute(status -> {
            try {
                log.info("开始编程式事务处理菜品更新，菜品ID: {}", dishDTO.getId());

                // 1. 验证菜品是否存在
                Dish existingDish = dishMapper.getById(dishDTO.getId());
                if (existingDish == null) {
                    throw new RuntimeException("菜品不存在，ID: " + dishDTO.getId());
                }

                // 2. 更新菜品基本信息
                Dish dish = new Dish();
                BeanUtils.copyProperties(dishDTO, dish);
                dish.setUpdateTime(LocalDateTime.now());
                dish.setUpdateUser(BaseContext.getCurrentId());

                dishMapper.updateDish(dish);

                // 3. 删除原有口味并插入新口味
                dishFlavorMapper.deleteByDishId(dishDTO.getId());
                List<DishFlavor> flavors = dishDTO.getFlavors();
                if (flavors != null && !flavors.isEmpty()) {
                    flavors.forEach(flavor -> {
                        flavor.setDishId(dishDTO.getId());
                    });
                    dishFlavorMapper.insertBatch(flavors);
                }

                return Result.success("菜品更新成功");
            } catch (Exception e) {
                status.setRollbackOnly();
                log.error("菜品ID: {} 更新失败", dishDTO.getId(), e);
                return Result.error("更新失败");
            }
        });
        log.info("结束编程式事务处理菜品更新，结果: {}", result);
        boolean flag = false;
        if (result.getCode() == 1) {
            // 更新后主动失效相关缓存，保障数据一致性
            try {
                // 获取菜品信息以确定分类ID
                Dish updatedDish = dishMapper.getById(dishDTO.getId());
                if (updatedDish != null) {
                    // 失效该分类下的所有菜品缓存
                    String cacheKey = RedisConstant.DISH_KEY + updatedDish.getCategoryId() + ":*";
                    // 这里可以扩展为使用Redis的keys或scan命令批量删除相关缓存
                    stringRedisTemplate.delete(cacheKey);
                    // 删除本地缓存
                    cacheKey = RedisConstant.DISH_KEY + updatedDish.getCategoryId() + ":" + updatedDish.getStatus();
                    localCache.delete(cacheKey);
                    flag = true;
                }
            } catch (Exception e) {
                log.warn("缓存失效操作失败，菜品ID: {}", dishDTO.getId(), e);
            }
        }
        return flag ? Result.success("菜品更新成功") : Result.error("更新失败");
    }

    @Override
    public List<Dish> list(Long categoryId) {
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        return dishMapper.list(dish);
    }

    /**
     * 起售/停售菜品
     *
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id) {
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        dishMapper.updateDish(dish);

        // 更新后主动失效相关缓存，保障数据一致性
        try {
            // 获取菜品信息以确定分类ID
            Dish updatedDish = dishMapper.getById(id);
            if (updatedDish != null) {
                // 失效该分类下的所有菜品缓存
                String cacheKey = RedisConstant.DISH_KEY + updatedDish.getCategoryId() + ":*";
                // 这里可以扩展为使用Redis的keys或scan命令批量删除相关缓存
                stringRedisTemplate.delete(cacheKey);
                log.info("菜品状态更新，失效分类ID: {} 的相关缓存", updatedDish.getCategoryId());
            }
        } catch (Exception e) {
            log.warn("缓存失效操作失败，菜品ID: {}", id, e);
        }
    }

    /**
     * 条件查询菜品和口味
     *
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        // 构建缓存Key：菜品分类ID + 状态
        String cacheKey = RedisConstant.DISH_KEY + dish.getCategoryId() + ":" + dish.getStatus();
        try {
            // 1.优先从本地缓存中获取数据
            Object cache = localCache.get(cacheKey);
            if (cache == null) {
                log.warn("本地缓存获取失败,降级Redis缓存{}", cacheKey);
                // 2.从Redis缓存中获取数据
                String json = stringRedisTemplate.opsForValue().get(cacheKey);
                if (json != null && !json.isEmpty()) {
                    log.info("从Redis缓存获取菜品数据，分类ID: {}, 状态: {}", dish.getCategoryId(), dish.getStatus());
                    return JSONUtil.toList(json, DishVO.class);
                }
            } else {
                return JSONUtil.toList(cache.toString(), DishVO.class);
            }
        } catch (Exception e) {
            log.warn("本地缓存和Redis缓存读取失败，降级查询数据库，key: {}", cacheKey, e);
        }

        // 直接查询数据库
        List<Dish> dishList = dishMapper.list(dish);
        List<DishVO> dishVOList = new ArrayList<>();
        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d, dishVO);
            // 根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());
            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        try {
            localCache.put(cacheKey, JSONUtil.toJsonStr(dishVOList), 30, TimeUnit.MINUTES);
            stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(dishVOList), 30, TimeUnit.MINUTES);
            log.info("菜品数据已缓存到Redis，分类ID: {}, 缓存数量: {}", dish.getCategoryId(), dishVOList.size());
        } catch (Exception e) {
            log.warn("Redis缓存写入失败，key: {}", cacheKey, e);
        }

        return dishVOList;
    }
}