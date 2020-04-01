package com.example.redis.controller;

import com.example.redis.config.JedisConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * @author v_vllchen
 */
@RestController
@RequestMapping("/test")
public class TestController {
    private Logger logger = LoggerFactory.getLogger(getClass());
    @Resource
    private JedisConfig jedisConfig;


    //总库存
    private long stock = 0;
    //商品key名字
    private String goodsKey = "goods_key";
    //获取锁的超时时间 秒
    private int timeout = 30 * 1000;

    @GetMapping("/getOrder")
    public List<String> getOrder() {
        //抢到商品的用户
        List<String> shopUsers = new ArrayList<>();
        //构造很多用户
        List<String> users = new ArrayList<>();
        IntStream.range(0, 100000).parallel().forEach(b -> {
            users.add("测试用户-" + b);
        });

        //初始化库存
        stock = 10;
        //模拟开抢
        users.parallelStream().forEach(b -> {
            String shopUser = start(b);
            if (!StringUtils.isEmpty(shopUser)) {
                shopUsers.add(shopUser);
            }
        });

        return shopUsers;
    }
    /**
     * 模拟抢单动作
     *
     * @param b
     * @return
     */
    private String start(String b) {
        //用户开抢时间
        long startTime = System.currentTimeMillis();

        //未抢到的情况下，30秒内继续获取锁
        while ((startTime + timeout) >= System.currentTimeMillis()) {
            //商品是否剩余
            if (stock <= 0) {
                break;
            }
            if (jedisConfig.setnx(goodsKey, b)) {
                //用户b拿到锁
                logger.info("用户{}拿到锁...", b);
                try {
                    //商品是否剩余
                    if (stock <= 0) {
                        break;
                    }
                    //模拟生成订单耗时操作，方便查看：神牛-50 多次获取锁记录
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    //抢购成功，商品递减，记录用户
                    stock -= 1;
                    //抢单成功跳出
                    logger.info("用户{}抢单成功跳出...所剩库存：{}", b, stock);

                    return b + "抢单成功，所剩库存：" + stock;
                } finally {
                    logger.info("用户{}释放锁...", b);
                    //释放锁
                    jedisConfig.delnx(goodsKey, b);
                }
            } else {
                //用户b没拿到锁，在超时范围内继续请求锁，不需要处理
//                if (b.equals("神牛-50") || b.equals("神牛-69")) {
//                    logger.info("用户{}等待获取锁...", b);
//                }
            }
        }
        return "";
    }

    @GetMapping("/setnx/{key}/{val}")
    public boolean setnx(@PathVariable String key, @PathVariable String val) {
        return jedisConfig.setnx(key, val);
    }
    @GetMapping("/delnx/{key}/{val}")
    public int delnx(@PathVariable String key, @PathVariable String val) {
        return jedisConfig.delnx(key, val);
    }


}
