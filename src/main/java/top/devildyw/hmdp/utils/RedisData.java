package top.devildyw.hmdp.utils;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author Devil
 * @since 2023-01-29-15:27
 */
@Data
public class  RedisData {
    private LocalDateTime expireTime;
    private Object data;
}
