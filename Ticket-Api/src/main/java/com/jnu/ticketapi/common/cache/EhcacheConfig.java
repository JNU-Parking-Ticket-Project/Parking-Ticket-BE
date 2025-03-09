package com.jnu.ticketapi.common.cache;


import javax.cache.Cache;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.spi.CachingProvider;
import org.ehcache.jsr107.EhcacheCachingProvider;
import org.springframework.cache.CacheManager;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author cookie ehcache config 객체입니다.(공지사항 상세조회 캐싱처리입니다)
 */
@Configuration
public class EhcacheConfig {

    /**
     * 추후 다른 캐시 도입시 충돌 방지를 위해 ehcacheProvider 명시적 지정했습니다. 캐시 중복 생성 방지하기 위해 null value validation
     * 추가했습니다. 값 복사는 비활성화해뒀고, 기본적으로 TTL은 10분으로 지정했지만 이부분은 논의해보고 변경하셔도 됩니다.
     *
     * <p>아래는 캐시 리소스 예상 cost입니다.
     *
     * <p>ResourcePool 기준 : 메모리량 = bytes * 객체 수 Long : 8bytes announceTitle : 8bytes, 문자열 오버헤드
     * 24bytes. title이 대략 50자라고 계산했을 때 50*2bytes = 100bytes. 즉 8+24+100 = 132bytes announceContent :
     * 대략 500자 기준 1032bytes LocalDateTime : 8bytes(timestamp) + 8(nanoseconds) + 16bytes(metadata) =
     * 32bytes imageUrls : 이미지 URL이 대략 10개 기준 평균 30자라고 했을 때, 10*8 + 10*24 + 10*30*2 = 928bytes 즉,
     * DTO당 2132bytes = 2.08KB 리소스 먹습니다 1000개로 설정했으니 약 2MB메모리 먹겠네요.
     */
    @Bean(name = "ehcacheManager")
    public CacheManager ehCacheManager() {

        CachingProvider cachingProvider =
                Caching.getCachingProvider(EhcacheCachingProvider.class.getName());

        javax.cache.CacheManager ehCacheManager = cachingProvider.getCacheManager();
        MutableConfiguration<Object, Object> cacheConfig =
                new MutableConfiguration<>()
                        .setStoreByValue(false)
                        .setExpiryPolicyFactory(
                                CreatedExpiryPolicy.factoryOf(Duration.TEN_MINUTES));

        Cache<Object, Object> cache = ehCacheManager.getCache("announceCache");
        if (cache == null) {
            ehCacheManager.createCache("announceCache", cacheConfig);
        }

        return new JCacheCacheManager(ehCacheManager);
    }
}
