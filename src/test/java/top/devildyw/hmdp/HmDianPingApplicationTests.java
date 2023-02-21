package top.devildyw.hmdp;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import top.devildyw.hmdp.entity.Shop;
import top.devildyw.hmdp.service.IShopService;
import top.devildyw.hmdp.utils.RedisData;
import top.devildyw.hmdp.utils.RedisIdWorker;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static top.devildyw.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
@Slf4j
@SpringBootTest
class HmDianPingApplicationTests {

    @Resource
    private IShopService shopService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisIdWorker idWorker;

    private static final ThreadPoolExecutor es = new ThreadPoolExecutor(10,20,10, TimeUnit.SECONDS,new LinkedBlockingDeque<Runnable>(),new ThreadPoolExecutor.DiscardPolicy());
    @Test
    public void saveShop2Redis(){
        int id = 1;
        //1. 查询店铺数据
        Shop shop = shopService.getById(id);
        //2. 封装逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(20));


        //3. 写入redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id, JSONUtil.toJsonStr(redisData));
    }

    @Test
    public void testIDWorker() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);

        Runnable task = ()->{
            for (int i = 0; i < 100; i++) {
                long order = idWorker.nextId("order");
                System.out.println("id:"+order);
            }
            latch.countDown();
        };

        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("time = "+(end-begin));
    }

}
