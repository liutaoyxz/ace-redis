-- k1 没有被抢的红包队列 , k2  已经抢到的红包队列,插入uid , k3 已经抢到的用户id 存放的map,用来判断用户是否抢到过   k4 uid,用户的id
-- 判断map 中是否已经存在了这个uid  如果存在直接返回取到的值,是红包的json字符串
local map = redis.call('hget',KEYS[3],KEYS[4])
--return map
if map then
    print(map)
    local mj = cjson.decode(map)
    mj['known'] = 1
    map = cjson.encode(mj)
    return map
end

if redis.call('llen',KEYS[1]) == 0 then
    return nil
end
local hb = redis.call('rpop',KEYS[1])
local hbj = cjson.decode(hb)
hbj.uid = KEYS[4]
hb = cjson.encode(hbj)
print(hb)
redis.call('lpush',KEYS[2],hb)
redis.call('hset',KEYS[3],KEYS[4],hb)
return hb



