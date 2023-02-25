package top.devildyw.cl_dianping.core;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import top.devildyw.cl_dianping.common.utils.RedisData;
import top.devildyw.cl_dianping.common.utils.RedisIdWorker;
import top.devildyw.cl_dianping.core.entity.Shop;
import top.devildyw.cl_dianping.core.service.IShopService;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static top.devildyw.cl_dianping.common.constants.RedisConstants.CACHE_SHOP_KEY;
import static top.devildyw.cl_dianping.common.constants.RedisConstants.SHOP_GEO_KEY;


@Slf4j
@SpringBootTest
class ClDianPingApplicationTests {

    private static final ThreadPoolExecutor es = new ThreadPoolExecutor(10, 20, 10, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(), new ThreadPoolExecutor.DiscardPolicy());
    @Resource
    private IShopService shopService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedisIdWorker idWorker;

    @Test
    public void saveShop2Redis() {
        int id = 1;
        //1. 查询店铺数据
        Shop shop = shopService.getById(id);
        //2. 封装逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(20));


        //3. 写入redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }

    @Test
    public void testIDWorker() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);

        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long order = idWorker.nextId("order");
                System.out.println("id:" + order);
            }
            latch.countDown();
        };

        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("time = " + (end - begin));
    }

    @Test
    public void loadShopData() {
        //1. 查询店铺信息
        List<Shop> list = shopService.list();
        //2. 按照类型对店铺分组 以typeId分组 todo 使用批量加入的方式
        for (Shop shop : list) {
            String key = SHOP_GEO_KEY + shop.getTypeId();
            stringRedisTemplate.opsForGeo().add(key, new Point(shop.getX(), shop.getY()), shop.getId().toString());
        }
        //
    }

    @Test
    public void test() {
        Long record = 1L;
        int count = 0;
        while (true) {
            if ((record & 1) == 0) {
                break;
            }
            count += 1;
            record >>>= 1;
        }

        System.out.println(count);
    }


    /** 新增秒杀券 todo:删除
     * {
     *     "shopId": 1,
     *     "title": "100元代金券",
     *     "subTitle": "周一至周日均可使用",
     *     "rules": "全场通用\\n无需预约\\n可无限叠加\\不兑现、不找零\\n仅限堂食",
     *     "payValue": 8000,
     *     "actualValue": 100000,
     *     "type": 1,
     *     "status":1,
     *     "stock":100,
     *     "beginTime":"2023-02-21T10:09:17",
     *     "endTime":"2023-02-28T10:09:17",
     *     "createTime":"2023-02-21T10:09:17",
     *     "updateTime":"2023-02-21T10:09:17"
     * }
     */
}
