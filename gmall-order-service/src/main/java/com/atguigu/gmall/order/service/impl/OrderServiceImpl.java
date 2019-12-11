package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.OmsOrderItem;
import com.atguigu.gmall.order.mapper.OmsOrderItemMapper;
import com.atguigu.gmall.order.mapper.OmsOrderMapper;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    OmsOrderMapper omsOrderMapper;

    @Autowired
    OmsOrderItemMapper omsOrderItemMapper;

    @Override
    public String checkTradeCode(String memberId, String tradeCode) {

        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();

            String key = "user:" + memberId + ":tradeCode";


            //对比防重删令牌
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

            Long eval = (Long) jedis.eval(script, Collections.singletonList(key), Collections.singletonList(tradeCode));

            if (eval != null && eval != 0) {
                jedis.del(key);     //需要使用lua脚本，查询到key的同时立即删除，，防止高并发情况下的订单攻击（大概率黑客攻击）
                return "success";
            } else {
                return "fail";
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            jedis.close();
        }

        return "fail";

    }

    @Override
    public String getTradeCode(String memberId) {
        Jedis jedis = null;
        String tradeCode = null;
        try {
            jedis = redisUtil.getJedis();

            String key = "user:" + memberId + ":tradeCode";
            tradeCode = UUID.randomUUID().toString();

            jedis.setex(key, 60 * 15, tradeCode);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            jedis.close();
        }
        return tradeCode;
    }

    @Override
    public void saveOrder(OmsOrder omsOrder) {

        //报讯订单 OmsOrder
        omsOrderMapper.insertSelective(omsOrder);

        //保存订单详情 OmsOrderItem
        String orderId = omsOrder.getId();
        List<OmsOrderItem> omsOrderItems = omsOrder.getOmsOrderItems();
        for (OmsOrderItem omsOrderItem : omsOrderItems) {
            omsOrderItem.setOrderId(orderId);
            omsOrderItemMapper.insertSelective(omsOrderItem);
        }
    }

    @Override
    public OmsOrder getOmsOrderByOutTradeNo(String outTradeNo) {

        OmsOrder omsOrder = new OmsOrder();
        omsOrder.setOrderSn(outTradeNo);
        OmsOrder omsOrderFromDb = omsOrderMapper.selectOne(omsOrder);

        return omsOrderFromDb;
    }
}
