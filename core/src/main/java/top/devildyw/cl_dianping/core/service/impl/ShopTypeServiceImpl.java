package top.devildyw.cl_dianping.core.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import top.devildyw.cl_dianping.common.DTO.Result;
import top.devildyw.cl_dianping.core.entity.ShopType;
import top.devildyw.cl_dianping.core.mapper.ShopTypeMapper;
import top.devildyw.cl_dianping.core.service.IShopTypeService;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static top.devildyw.cl_dianping.common.constants.RedisConstants.CACHE_SHOP_TYPE_KEY;
import static top.devildyw.cl_dianping.common.constants.RedisConstants.CACHE_SHOP_TYPE_TTL;


/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author Devil
 * @since 2023-01-11-15:35
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {
        String cacheKey = CACHE_SHOP_TYPE_KEY;

        //1. 查询 Redis 缓存
        String shopTypeJson = stringRedisTemplate.opsForValue().get(cacheKey);
        //2. 查看缓存是否命中
        if (StringUtils.hasText(shopTypeJson)) {
            //3. 命中，直接返回
            List<ShopType> shopTypes = JSONUtil.toList(shopTypeJson, ShopType.class);
            return Result.ok(shopTypes);
        }

        //4. 未命中，数据库中查询
        List<ShopType> shopTypes = query().orderByAsc("sort").list();

        //5. 数据库中没有则直接返回错误信息
        if (shopTypes.isEmpty()) {
            return Result.ok(shopTypes);
        }

        //6. 有则存入 redis 缓存
        stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(shopTypes), CACHE_SHOP_TYPE_TTL, TimeUnit.MINUTES);

        //7.返回数据
        return Result.ok(shopTypes);

    }
}
