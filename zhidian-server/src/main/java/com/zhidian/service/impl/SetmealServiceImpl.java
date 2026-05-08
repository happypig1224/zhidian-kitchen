package com.zhidian.service.impl;

import cn.hutool.json.JSONUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zhidian.constant.MessageConstant;
import com.zhidian.constant.StatusConstant;
import com.zhidian.dto.SetmealDTO;
import com.zhidian.dto.SetmealPageQueryDTO;
import com.zhidian.entity.Dish;
import com.zhidian.entity.Setmeal;
import com.zhidian.entity.SetmealDish;
import com.zhidian.exception.DeletionNotAllowedException;
import com.zhidian.exception.SetmealEnableFailedException;
import com.zhidian.mapper.DishMapper;
import com.zhidian.mapper.SetmealDishMapper;
import com.zhidian.mapper.SetmealMapper;
import com.zhidian.result.PageResult;
import com.zhidian.result.Result;
import com.zhidian.service.SetmealService;
import com.zhidian.utils.RedisConstant;
import com.zhidian.vo.DishItemVO;
import com.zhidian.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.zhidian.utils.RedisConstant.SETMEAL_DISH_KEY;

/**
 * 套餐业务实现
 */
@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * 新增套餐，同时需要保存套餐和菜品的关联关系
     *
     * @param setmealDTO
     */
    public Result<String> saveWithDish(SetmealDTO setmealDTO) {
        Result<String> result = transactionTemplate.execute(status -> {
            try {
                Setmeal setmeal = new Setmeal();
                BeanUtils.copyProperties(setmealDTO, setmeal);

                //向套餐表插入数据
                setmealMapper.insert(setmeal);

                //获取生成的套餐id
                Long setmealId = setmeal.getId();

                List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
                setmealDishes.forEach(setmealDish -> {
                    setmealDish.setSetmealId(setmealId);
                });

                //保存套餐和菜品的关联关系
                setmealDishMapper.insertBatch(setmealDishes);
                return Result.success("新增套餐成功");
            } catch (Exception e) {
                status.setRollbackOnly();
                log.error("新增套餐失败", e);
                return Result.error("新增套餐失败," + e);
            }
        });

        // Redis缓存操作放在事务外，避免影响数据库事务性能
        if (result.getCode() == 1) { // 成功时处理缓存
            try {
                Setmeal lastSetmeal = setmealMapper.getLastInsert();
                if (lastSetmeal != null) {
                    stringRedisTemplate.opsForValue().set(SETMEAL_DISH_KEY + lastSetmeal.getId(), 
                        JSONUtil.toJsonStr(setmealDTO.getSetmealDishes()), 
                        RedisConstant.CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
                }
            } catch (Exception e) {
                log.warn("Redis缓存写入失败，但不影响主流程", e);
            }
        }
        
        return result;
    }

    /**
     * 分页查询
     *
     * @param setmealPageQueryDTO
     * @return
     */
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        int pageNum = setmealPageQueryDTO.getPage();
        int pageSize = setmealPageQueryDTO.getPageSize();

        PageHelper.startPage(pageNum, pageSize);
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 修改套餐
     *
     * @param setmealDTO
     */
    public Result<String> update(SetmealDTO setmealDTO) {
        Result<String> result = transactionTemplate.execute(status -> {
            try {
                Setmeal setmeal = new Setmeal();
                BeanUtils.copyProperties(setmealDTO, setmeal);

                //1、修改套餐表，执行update
                setmealMapper.update(setmeal);

                //套餐id
                Long setmealId = setmealDTO.getId();

                //2、删除套餐和菜品的关联关系，操作setmeal_dish表，执行delete
                setmealDishMapper.deleteBySetmealId(setmealId);

                List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
                setmealDishes.forEach(setmealDish -> {
                    setmealDish.setSetmealId(setmealId);
                });
                //3、重新插入套餐和菜品的关联关系，操作setmeal_dish表，执行insert
                setmealDishMapper.insertBatch(setmealDishes);
                return Result.success("修改套餐成功");
            } catch (Exception e) {
                status.setRollbackOnly();
                log.error("修改套餐失败", e);
                return Result.error("修改套餐失败," + e);
            }
        });

        // Redis缓存操作放在事务外
        if (result.getCode() == 1) { // 成功时处理缓存
            try {
                stringRedisTemplate.delete(SETMEAL_DISH_KEY + setmealDTO.getId());
            } catch (Exception e) {
                log.warn("Redis缓存删除失败，但不影响主流程", e);
            }
        }
        
        return result;
    }

    @Override
    public SetmealVO getById(Long id) {
        Setmeal setmeal = setmealMapper.getById(id);
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        System.out.println("setmealVO = " + setmealVO);
        List<SetmealDish> setmealDishes = setmealDishMapper.getById(setmealVO.getId());
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }

    @Override
    public void startorstop(Integer status, Long id) {
        //起售套餐时，判断套餐内是否有停售菜品，有停售菜品提示"套餐内包含未启售菜品，无法启售"
        if (status == StatusConstant.ENABLE) {
            //select a.* from dish a left join setmeal_dish b on a.id = b.dish_id where b.setmeal_id = ?
            List<Dish> dishList = dishMapper.getBySetmealId(id);
            if (dishList != null && dishList.size() > 0) {
                dishList.forEach(dish -> {
                    if (StatusConstant.DISABLE == dish.getStatus()) {
                        throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                });
            }
        }

        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        setmealMapper.update(setmeal);
    }

    public Result<String> delete(List<Long> ids) {
        List<Long> successIds = new ArrayList<>();
        List<Long> failedIds = new ArrayList<>();
        
        for (Long id : ids) {
            try {
                // 每个套餐独立事务
                Result<String> singleResult = transactionTemplate.execute(status -> {
                    try {
                        Setmeal setmeal = setmealMapper.getById(id);
                        if (setmeal.getStatus() == StatusConstant.ENABLE) {
                            throw new DeletionNotAllowedException("起售中商品" + setmeal.getName() + "不能删除");
                        }
                        
                        setmealMapper.deleteSetmealById(id);
                        setmealDishMapper.deleteBySetmealId(id);
                        successIds.add(id);
                        return Result.success("删除套餐成功");
                    } catch (Exception e) {
                        status.setRollbackOnly();
                        failedIds.add(id);
                        log.error("删除套餐失败，ID: {}", id, e);
                        return Result.error("删除套餐失败");
                    }
                });
            } catch (Exception e) {
                failedIds.add(id);
                log.error("删除套餐事务执行失败，ID: {}", id, e);
            }
        }
        
        // Redis缓存清理放在事务外
        successIds.forEach(setmealId -> {
            try {
                stringRedisTemplate.delete(SETMEAL_DISH_KEY + setmealId);
            } catch (Exception e) {
                log.warn("Redis缓存删除失败，ID: {}", setmealId, e);
            }
        });
        
        if (!failedIds.isEmpty()) {
            return Result.error("部分删除失败，成功删除: " + successIds.size() + "个，失败: " + failedIds.size() + "个，失败ID: " + failedIds);
        }
        return Result.success("批量删除成功，共删除" + successIds.size() + "个套餐");
    }

    /**
     * 条件查询
     *
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     *
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        // 构建缓存Key：套餐ID
        String cacheKey = RedisConstant.SETMEAL_DISH_KEY + id;

        try {
            // 1.优先从Redis缓存中获取数据
            String cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cacheValue != null && !cacheValue.isEmpty()) {
                log.info("从Redis缓存获取套餐菜品数据，套餐ID: {}", id);
                return JSONUtil.toList(cacheValue, DishItemVO.class);
            }
        } catch (Exception e) {
            log.warn("Redis缓存读取失败，降级查询数据库，key: {}", cacheKey, e);
        }

        // 直接查询数据库
        List<DishItemVO> list = setmealMapper.getDishItemBySetmealId(id);

        try {
            stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(list), 30, TimeUnit.MINUTES);
            log.info("套餐菜品数据已缓存到Redis，套餐ID: {}, 菜品数量: {}", id, list.size());
        } catch (Exception e) {
            log.warn("Redis缓存写入失败，key: {}", cacheKey, e);
        }

        return list;
    }

}