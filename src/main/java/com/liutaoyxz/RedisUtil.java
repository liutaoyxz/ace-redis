package com.liutaoyxz;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

import java.util.ArrayList;
import java.util.List;

/**
 * @author liutao
 * @description :
 * @create 2017-03-24 11:26
 */
public class RedisUtil {


    private static final ShardedJedisPool POOL ;


    static {
        JedisShardInfo info1 = new JedisShardInfo("192.168.162.128",6379,"r1");
        List<JedisShardInfo> infos = new ArrayList<JedisShardInfo>();
        infos.add(info1);
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMinIdle(30);
        config.setMaxIdle(100);
        POOL = new ShardedJedisPool(config,infos);
    }


    public static Jedis getJedis(){
        Jedis jedis = POOL.getResource().getShard("r1");
        return jedis;
    }


    public static void main(String[] args) {

    }



}
