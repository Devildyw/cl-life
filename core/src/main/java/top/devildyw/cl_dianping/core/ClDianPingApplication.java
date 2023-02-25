package top.devildyw.cl_dianping.core;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

//开启aspectJ代理 exposeProxy = true 使其支持通过 AopContext 访问当前类得代理对象
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("top.devildyw.cl_dianping.core.mapper")
@SpringBootApplication(scanBasePackages = "top.devildyw.cl_dianping.*")
public class ClDianPingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClDianPingApplication.class, args);
    }

}
