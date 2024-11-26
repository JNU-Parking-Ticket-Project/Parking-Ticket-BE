package com.jnu.ticketapi.Announce.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@Import(CacheTestConfiguration.class) // 테스트 설정 클래스를 로드
@EnableCaching
public class CacheTest {

    @Autowired private CacheService sampleService;

    @Autowired private CacheManager ehCacheManager;

    @Test
    public void testCache() {
        // 첫 번째 호출, 캐시가 비어 있으므로 서비스 메서드가 실행됩니다.
        long start = System.currentTimeMillis();
        String result1 = sampleService.getCachedData("testParam");
        long duration1 = System.currentTimeMillis() - start;

        // 두 번째 호출, 캐시가 적용되어 즉시 응답합니다.
        start = System.currentTimeMillis();
        String result2 = sampleService.getCachedData("testParam");
        long duration2 = System.currentTimeMillis() - start;

        // 결과 확인
        assertThat(result1).isEqualTo(result2);
        assertThat(duration2).isLessThan(duration1); // 두 번째 호출은 첫 번째 호출보다 빠름
    }

    @Test
    public void testCacheEviction() {
        // 캐시에 데이터를 추가합니다.
        sampleService.getCachedData("evictTest");

        // 캐시에 데이터가 있는지 확인합니다.
        assertThat(ehCacheManager.getCache("myCache").get("evictTest")).isNotNull();

        // 캐시를 제거합니다.
        ehCacheManager.getCache("myCache").clear();

        // 캐시가 비어 있는지 확인합니다.
        assertThat(ehCacheManager.getCache("myCache").get("evictTest")).isNull();
    }
}
