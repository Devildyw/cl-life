package top.devildyw.hmdp;

import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import top.devildyw.hmdp.entity.Shop;
import top.devildyw.hmdp.service.IShopService;
import top.devildyw.hmdp.utils.RedisData;

import javax.annotation.Resource;
import java.time.LocalDateTime;

import static top.devildyw.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

@SpringBootTest
class HmDianPingApplicationTests {

    @Resource
    private IShopService shopService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
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


}
