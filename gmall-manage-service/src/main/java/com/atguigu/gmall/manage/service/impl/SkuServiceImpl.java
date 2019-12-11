package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.PmsSkuAttrValue;
import com.atguigu.gmall.bean.PmsSkuImage;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.bean.PmsSkuSaleAttrValue;
import com.atguigu.gmall.manage.mapper.PmsSkuAttrValueMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuImageMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuInfoMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuSaleAttrValueMapper;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class SkuServiceImpl implements SkuService {

    @Autowired
    private PmsSkuInfoMapper pmsSkuInfoMapper;

    @Autowired
    private PmsSkuAttrValueMapper pmsSkuAttrValueMapper;

    @Autowired
    private PmsSkuSaleAttrValueMapper pmsSkuSaleAttrValueMapper;

    @Autowired
    private PmsSkuImageMapper pmsSkuImageMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public void saveSkuInfo(PmsSkuInfo pmsSkuInfo) {

        //插入 skuInfo
        pmsSkuInfoMapper.insertSelective(pmsSkuInfo);

        //skuId
        String skuId = pmsSkuInfo.getId();

        //插入平台属性关联
        List<PmsSkuAttrValue> skuAttrValueList = pmsSkuInfo.getSkuAttrValueList();
        for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
            pmsSkuAttrValue.setSkuId(skuId);
            pmsSkuAttrValueMapper.insertSelective(pmsSkuAttrValue);
        }

        //插入销售属性关联
        List<PmsSkuSaleAttrValue> skuSaleAttrValueList = pmsSkuInfo.getSkuSaleAttrValueList();
        for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList) {
            pmsSkuSaleAttrValue.setSkuId(skuId);
            pmsSkuSaleAttrValueMapper.insertSelective(pmsSkuSaleAttrValue);
        }

        //插入 sku图片
        List<PmsSkuImage> skuImageList = pmsSkuInfo.getSkuImageList();
        for (PmsSkuImage pmsSkuImage : skuImageList) {
            pmsSkuImage.setSkuId(skuId);
            pmsSkuImageMapper.insertSelective(pmsSkuImage);
        }
    }

    public PmsSkuInfo getSkuByIdFromDb(String skuId) {

        //sku 商品信息
        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
        pmsSkuInfo.setId(skuId);
        PmsSkuInfo skuInfo = pmsSkuInfoMapper.selectByPrimaryKey(pmsSkuInfo);

        //sku 图片列表
        PmsSkuImage pmsSkuImage = new PmsSkuImage();
        pmsSkuImage.setSkuId(skuId);
        List<PmsSkuImage> pmsSkuImageList = pmsSkuImageMapper.select(pmsSkuImage);
        skuInfo.setSkuImageList(pmsSkuImageList);

//        //sku 属性值列表
//        PmsSkuAttrValue pmsSkuAttrValue = new PmsSkuAttrValue();
//        pmsSkuAttrValue.setSkuId(skuId);
//        List<PmsSkuAttrValue> pmsSkuAttrValueList = pmsSkuAttrValueMapper.select(pmsSkuAttrValue);
//        skuInfo.setSkuAttrValueList(pmsSkuAttrValueList);
//
//        //sku 销售属性值列表
//        PmsSkuSaleAttrValue pmsSkuSaleAttrValue = new PmsSkuSaleAttrValue();
//        pmsSkuSaleAttrValue.setSkuId(skuId);
//        List<PmsSkuSaleAttrValue> pmsSkuSaleAttrValueList = pmsSkuSaleAttrValueMapper.select(pmsSkuSaleAttrValue);
//        skuInfo.setSkuSaleAttrValueList(pmsSkuSaleAttrValueList);

        return skuInfo;
    }

    @Override
    public PmsSkuInfo getSkuById(String skuId, String ip) {

        System.out.println("ip为：" + ip + "的同学：" + Thread.currentThread().getName() + "进入商品详情的请求。");

        //sku 商品信息
        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();

        //连接缓存
        Jedis jedis = redisUtil.getJedis();

        //查询缓存
        String k = "sku:" + skuId + ":info";
        String skuJson = jedis.get(k);

        if(StringUtils.isNotBlank(skuJson)){    //作用等同于 if(skuJson != null && !skuJson.equals("")

            System.out.println("ip为：" + ip + "的同学：" + Thread.currentThread().getName() + "从缓存中获得商品详情。");

            pmsSkuInfo = JSON.parseObject(skuJson, PmsSkuInfo.class);
        }else {
            //如果缓存没有，则从MySQL中查询
            System.out.println("ip为：" + ip + "的同学：" + Thread.currentThread().getName() + "发现缓存中没有，申请缓存的分布式锁："+ "sku:"+skuId+":lock");

            //设置分布式锁
            String token = UUID.randomUUID().toString();
            String OK = jedis.set("sku:"+skuId+":lock", token, "nx", "px", 60*1000*10);//拿到锁的线程有十秒的过期时间
            if(StringUtils.isNotBlank(OK) && OK.equals("OK")){

                //设置成功，有权在10秒的过期时间内访问MySQL
                System.out.println("ip为：" + ip + "的同学：" + Thread.currentThread().getName() + "成功拿到锁，有权在10秒的过期时间内访问MySQL");

                pmsSkuInfo = getSkuByIdFromDb(skuId);

                try {
                    Thread.sleep(10*1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                //MySQL查询结果存入redis
                if(pmsSkuInfo != null){
                    jedis.set("sku:" + skuId + ":info", JSON.toJSONString(pmsSkuInfo));
                }else {
                    jedis.setex("sku:" + skuId + ":info", 60*3, JSON.toJSONString(""));
                }

                System.out.println("ip为：" + ip + "的同学：" + Thread.currentThread().getName() + "使用完毕，将锁归还");
                //在访问MySQL后，将分布式锁释放掉
                String lockToken = jedis.get("sku:" + skuId + ":lock");
                if(StringUtils.isNotBlank(lockToken) && lockToken.equals(token)){   //用token确认删除的是自己的锁
                    // jedis.eval("lua");  使用lua脚本，可以在查询到key的同时删掉它，避免高并发的情况下意外的发生。
                    jedis.del("sku:"+skuId+":lock");
                }

            }else {
                //设置失败，自旋（该线程在睡眠几秒后，重新尝试访问该方法）
                System.out.println("ip为：" + ip + "的同学：" + Thread.currentThread().getName() + "没有拿到锁，开始自旋");

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return getSkuById(skuId, ip);
            }
        }

        jedis.close();

        return pmsSkuInfo;
    }


    @Override
    public List<PmsSkuInfo> getSkuSaleAttrValueListBySpu(String productId) {

        List<PmsSkuInfo> pmsSkuInfoList = pmsSkuInfoMapper.selectSkuSaleAttrValueListBySpu(productId);
        return pmsSkuInfoList;
    }

    @Override
    public List<PmsSkuInfo> getAllSku() {

        List<PmsSkuInfo> pmsSkuInfoList = pmsSkuInfoMapper.selectAll();
        for (PmsSkuInfo pmsSkuInfo : pmsSkuInfoList) {
            PmsSkuAttrValue pmsSkuAttrValue = new PmsSkuAttrValue();
            pmsSkuAttrValue.setSkuId(pmsSkuInfo.getId());
            List<PmsSkuAttrValue> pmsSkuAttrValueList = pmsSkuAttrValueMapper.select(pmsSkuAttrValue);
            pmsSkuInfo.setSkuAttrValueList(pmsSkuAttrValueList);
        }
        return pmsSkuInfoList;
    }

    @Override
    public boolean checkPrice(String productSkuId, BigDecimal price) {

        boolean b = false;

        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
        pmsSkuInfo.setId(productSkuId);
        PmsSkuInfo pmsSkuInfo1 = pmsSkuInfoMapper.selectOne(pmsSkuInfo);

        BigDecimal realPrice = pmsSkuInfo1.getPrice();

        if (realPrice.compareTo(price) == 0) {
            b = true;
        }

        return b;
    }
}
