package com.zhidian.utils;

/**
 * Redis缓存常量定义
 *
 * @Author 吴汇明
 * @School 绥化学院
 * @CreateTime
 */
public class RedisConstant {
    // 套餐菜品缓存Key
    public static final String SETMEAL_DISH_KEY = "setmeal:dish:";

    // 菜品缓存Key
    public static final String DISH_KEY = "dish:";

    // 购物车缓存Key
    public static final String SHOPPING_CART_KEY = "shopping:cart:";

    // 购物车联合索引缓存Key（用户ID + 菜品/套餐ID）
    public static final String CART_INDEX_KEY = "cart:index:";

    // 缓存过期时间配置
    public static final long CACHE_EXPIRE_MINUTES = 30;
    public static final long CART_CACHE_EXPIRE_MINUTES = 30;
    public static final String  SECKILL_VOUCHER_KEY = "seckill:voucher:";
    public static final String SECKILL_VOUCHER_LIMIT_KEY = "seckill:voucher:limit:";
    public static final String SECKILL_USER_KEY = "seckill:user:";
    public static final String SECKILL_VOUCHER_BEGIN_KEY = "seckill:voucher:begin:";
    public static final String SECKILL_LOCK_KEY = "seckill:lock:";
    public static final String SECKILL_ORDER_KEY="seckill:order:";
    public static final String SECKILL_STOCK_KEY = "seckill:stock";
}