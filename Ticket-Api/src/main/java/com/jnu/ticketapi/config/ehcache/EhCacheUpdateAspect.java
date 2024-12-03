package com.jnu.ticketapi.config.ehcache;


import com.jnu.ticketapi.api.announce.model.response.AnnounceDetailsResponse;
import com.jnu.ticketapi.api.announce.model.response.UpdateAnnounceResponse;
import java.time.LocalDateTime;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class EhCacheUpdateAspect {

    private final CacheManager cacheManager;

    public EhCacheUpdateAspect(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /** 캐시 업데이트 */
    @AfterReturning(pointcut = "@annotation(cacheUpdate)", returning = "result")
    public void updateCache(JoinPoint joinPoint, CacheUpdate cacheUpdate, Object result) {
        Cache cache = cacheManager.getCache(cacheUpdate.cacheName());
        if (cache == null) {
            throw new IllegalArgumentException(
                    "Cache " + cacheUpdate.cacheName() + " 가 존재하지 않습니다."); // TODO 추후 커스텀 양식대로 수정
        }
        AnnounceDetailsResponse oldAnnounceDetails =
                cache.get(cacheUpdate.key(), AnnounceDetailsResponse.class); // 기존 캐시 데이터

        // key의 값과 같은 파라미터이름을 찾아서 announceId를 찾는다.
        String[] parameterNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
        long announceId = -1L;
        for (int idx = 0; idx < parameterNames.length; idx++) {
            if (parameterNames[idx].equals(cacheUpdate.key())) {
                announceId = Long.parseLong(joinPoint.getArgs()[idx].toString());
            }
        }

        if (result instanceof UpdateAnnounceResponse updateAnnounceResponse) {
            assert oldAnnounceDetails != null; // TODO 추후 메서드 추출
            AnnounceDetailsResponse newAnnounceDetails =
                    convertToAnnounceDetailsResponse(
                            updateAnnounceResponse,
                            announceId,
                            oldAnnounceDetails.announceCreatedAt());
            cache.put(cacheUpdate.key(), newAnnounceDetails);
        }
    }

    // TODO 인수 줄이기
    /** UpdateAnnounceResponse, key, createdAt -> AnnounceDetailsResponse 로 변환 합니다. */
    private AnnounceDetailsResponse convertToAnnounceDetailsResponse(
            UpdateAnnounceResponse updateAnnounceResponse,
            Long announceId,
            LocalDateTime createdAt) {
        // Post 엔티티를 GetDTO로 변환

        return AnnounceDetailsResponse.builder()
                .announceId(announceId)
                .announceTitle(updateAnnounceResponse.announceTitle())
                .announceContent(updateAnnounceResponse.announceContent())
                .imageUrls(updateAnnounceResponse.imageUrls())
                .announceCreatedAt(createdAt)
                .build();
    }
}
