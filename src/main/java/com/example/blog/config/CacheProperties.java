package com.example.blog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Data
@Configuration
@ConfigurationProperties(prefix = "blog.cache")
public class CacheProperties {

    /** 基础过期时间（分钟） */
    private int baseTtlMinutes = 30;

    /** 随机抖动范围（分钟），防止缓存同时失效 */
    private int jitterMinutes = 10;

    /** 空值缓存时间（分钟），防止缓存穿透 */
    private int nullTtlMinutes = 2;

    /** 分布式锁等待时间（毫秒） */
    private long lockWaitMs = 100;

    /** 分布式锁过期时间（秒） */
    private long lockExpireSeconds = 10;
}
