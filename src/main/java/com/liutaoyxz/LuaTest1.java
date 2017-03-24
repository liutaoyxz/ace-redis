package com.liutaoyxz;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.testng.annotations.Test;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author liutao
 * @description :
 * @create 2017-03-24 11:25
 */
@Test
public class LuaTest1 {

    public static String HONGBAO_SCRIPT;
    //生成的红包存放处
    public static String HONGBAO_LIST = "EMPTYLIST";
    //抢红包的uid 存放map
    public static String HASH = "HASH";
    //已经抢到的list
    public static String HAVE_LIST = "HAVELIST";

    public static int THREAD_NUM = 20;

    public static Random random = new Random();

    static {
        try {
            InputStream in = LuaTest1.class.getClassLoader().getResource("hongbao.lua").openStream();
            byte[] bytes = new byte[1024];
            int i;
            StringBuilder sb = new StringBuilder();
            while ((i = in.read(bytes)) != -1) {
                sb.append(new String(bytes, 0, i));
            }
            HONGBAO_SCRIPT = sb.toString();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {

        createHB(100, 10);
    }


    /**
     * 红包模拟程序
     * 一个随机生成红包的方法, 参数 红包总金额  红包数量. 要求每个红包都应该有钱,最少1元.
     * 然后使用20个线程一起去抢红包,最后统计抢到的钱和数量是不是和发时候的一样
     * 抢红包使用lua 脚本,如果抢到了返回红包对象. 如果已经抢光了返回null
     * 最后输出结果
     *
     */

    /**
     * 生成红包,操作之前先清空redis..  妈的这个算法有问题, 一点都不平均,数额大的话明显前几个生成的大很多....... 先这样吧
     *
     * @param amount 总金额
     * @param count  红包数量
     */
    public static void createHB(int amount, int count) {
        Jedis jedis = RedisUtil.getJedis();
        String[] list = new String[count];
        int surplus = count;
        for (int i = 0; i < count; i++) {
            int money;
            if (i == count - 1) {
                money = amount;
            } else {
                int max = amount - surplus + 1;
                money = randLevel(1, max);
            }
            amount -= money;
            Map<String,String> map = new HashMap<String, String>();
            map.put("hid", ""+(i + 1));
            map.put("money", ""+money);
            map.put("uid", "0");
            map.put("known", "0");
            String json = JSON.toJSON(map).toString();
            list[i] = json;
            surplus--;
        }
        jedis.flushDB();
        jedis.lpush(HONGBAO_LIST, list);
        jedis.close();
    }

    /**
     * 返回范围内的随机数
     *
     * @param min 包含下限
     * @param max 包含上限
     * @return 随机数
     */
    private static int randLevel(int min, int max) {
        if (max < min || min < 0) throw new IllegalArgumentException("max --> " + max + ",min --> " + min);
        return random.nextInt(max - min + 1) + min;
    }


    @Test
    public void testLua1() {

        Jedis jedis = RedisUtil.getJedis();
        Object eval = jedis.eval(HONGBAO_SCRIPT, 1, HONGBAO_LIST);
        System.out.println(eval);
    }

    @Test
    public void createHBtt() {
        LuaTest1.createHB(100, 10);
    }


    @Test
    public void qhb() throws InterruptedException {

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_NUM);
        final CountDownLatch latch = new CountDownLatch(THREAD_NUM);
        final List<String> hname = new CopyOnWriteArrayList<String>();
        final AtomicInteger amt = new AtomicInteger(0);
        for (int i = 0; i < THREAD_NUM; i++) {
            executorService.execute(new Runnable() {
                public void run() {
                    Jedis jedis = new Jedis("192.168.162.128",6379);
                    latch.countDown();
                    String eval = (String)jedis.eval(HONGBAO_SCRIPT, 4, HONGBAO_LIST, HAVE_LIST, HASH, Thread.currentThread().getName());
                    if (eval == null){
                        System.out.println(eval);
                        return ;
                    }
                    JSONObject json = JSON.parseObject(eval);
                    String k = json.getString("known");
                    if (k.equals("1")){
                        return ;
                    }
                    int money = json.getInteger("money");
                    amt.addAndGet(Integer.valueOf(money));
                    hname.add(Thread.currentThread().getName());
                }
            });
        }
        latch.await();
        System.out.println("抢到红包的用户有"+hname.size()+"个,用户列表是 --> "+hname);
        System.out.println("总共抢到了 -->"+amt.get() +"这些钱");

    }



}
