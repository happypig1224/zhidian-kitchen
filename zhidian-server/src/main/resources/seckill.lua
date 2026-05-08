-- KEYS[1]: 优惠券库存 key
-- KEYS[2]: 用户购买记录 key

local stock_key = KEYS[1]
local user_key = KEYS[2]

local stock = redis.call('get', stock_key)
if not stock or tonumber(stock) <= 0 then
    return -1  -- 库存不足
end

-- 检查用户是否已购买（使用setnx检查，避免两次网络往返）
local bought = redis.call('setnx', user_key, '1')
if bought == 0 then
    return -2  -- 已领取过
end

-- 设置用户购买记录过期时间
redis.call('expire', user_key, 24 * 60 * 60)

-- 扣减库存
redis.call('decr', stock_key)

return 1  -- 领取成功