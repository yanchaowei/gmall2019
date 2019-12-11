package com.atguigu.gmall.seckill.controller;

import com.atguigu.gmall.util.RedisUtil;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;

@Controller
public class SeckillController {

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    RedissonClient redissonClient;

    @RequestMapping("secKill")
    @ResponseBody
    public String secKill() {
        Jedis jedis = redisUtil.getJedis();


        int stock = Integer.parseInt(jedis.get("106"));

        if (stock > 0) {
            RSemaphore semaphore = redissonClient.getSemaphore("106");
            boolean b = semaphore.tryAcquire();
            if (b) {
                System.out.println("==========某位用户抢购成功，剩余库存数：" + (stock - 1));
            } else {
                System.out.println("抢购失败。");
            }
        }

        jedis.close();

        return "1";
    }

    @RequestMapping("kill")
    @ResponseBody
    public String kill() {
        Jedis jedis = redisUtil.getJedis();

        jedis.watch("106");
        int stock = Integer.parseInt(jedis.get("106"));

        if (stock > 0) {

            Transaction multi = jedis.multi();
            multi.incrBy("106", -1);
            List<Object> exec = multi.exec();
            if (exec != null && exec.size() > 0) {
                System.out.println("==========某位用户抢购成功，剩余库存数：" + stock);
            } else {
                System.out.println("抢购失败。");
            }
        }

        jedis.close();

        return "1";
    }
}
