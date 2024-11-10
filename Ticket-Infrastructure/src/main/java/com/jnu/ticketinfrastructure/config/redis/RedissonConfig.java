package com.jnu.ticketinfrastructure.config.redis;


import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.grid.jcache.JCacheProxyManager;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import lombok.RequiredArgsConstructor;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.jcache.configuration.RedissonConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@ConditionalOnExpression("${ableRedisson:true}")
public class RedissonConfig {

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private Integer redisPort;

    private static final String REDISSON_HOST_PREFIX = "redis://";

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer().setAddress(REDISSON_HOST_PREFIX + redisHost + ":" + redisPort);
        return Redisson.create(config);
    }
    /** for bucket4j */
    @Bean
    public CacheManager cacheManager(RedissonClient redissonClient) {
        CachingProvider cachingProvider =
                Caching.getCachingProvider("org.redisson.jcache.JCachingProvider");
        CacheManager manager = cachingProvider.getCacheManager();
        Cache<Object, Object> bucket4j = manager.getCache("bucket4j");
        if (bucket4j == null) {
            manager.createCache("bucket4j", RedissonConfiguration.fromInstance(redissonClient));
        }
        return manager;
    }

    /** for bucket4j */
    @Bean
    ProxyManager<String> proxyManager(CacheManager cacheManager) {
        return new JCacheProxyManager<>(cacheManager.getCache("bucket4j"));
    }
}
