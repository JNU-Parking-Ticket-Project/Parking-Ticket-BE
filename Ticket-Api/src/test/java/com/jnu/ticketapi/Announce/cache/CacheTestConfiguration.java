package com.jnu.ticketapi.Announce.cache;


import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.spi.CachingProvider;
import org.ehcache.jsr107.EhcacheCachingProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class CacheTestConfiguration {

    @Bean
    public CacheService sampleService() {
        return new CacheService();
    }

    @Bean
    public CacheManager ehCacheManager() {
        CachingProvider cachingProvider =
                Caching.getCachingProvider(EhcacheCachingProvider.class.getName());

        javax.cache.CacheManager ehCacheManager = cachingProvider.getCacheManager();
        MutableConfiguration<Object, Object> cacheConfig =
                new MutableConfiguration<>()
                        .setStoreByValue(false)
                        .setExpiryPolicyFactory(
                                CreatedExpiryPolicy.factoryOf(Duration.TEN_MINUTES));

        if (ehCacheManager.getCache("myCache") == null) {
            ehCacheManager.createCache("myCache", cacheConfig);
        }
        return new JCacheCacheManager(ehCacheManager);
    }
}
