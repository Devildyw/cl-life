package top.devildyw.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Devil
 * @since 2023-02-23-12:28
 */
@Configuration
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(){
        //配置类
        Config config = new Config();
        //配置单节点redis,也可以使用 useClusterServers()添加集群配置
        config.useSingleServer().setAddress("redis://124.222.35.20:6666").setPassword("dyw20020304");
        // 创建客户端
        return Redisson.create(config);
    }
}
