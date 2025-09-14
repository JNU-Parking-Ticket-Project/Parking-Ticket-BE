package com.jnu.ticketinfrastructure.service.queue;

import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class SectorThreadPoolManager {

    private final Map<Long, ExecutorService> sectorExecutors = new ConcurrentHashMap<>();
    private final Map<Long, BlockingQueue<Runnable>> sectorQueues = new ConcurrentHashMap<>();
    private final SectorAdaptor sectorAdaptor;

    /**
     * Event OPEN 될 때 해당 Event의 모든 Sector에 대해 스레드풀 미리 생성
     */
    public void initializeSectorThreadPools(Long eventId) {
        List<Sector> eventSectors = sectorAdaptor.findByEventId(eventId);

        for (Sector sector : eventSectors) {
            createThreadPoolForSector(sector.getId());
        }

        log.info("Event {} - 구간별 스레드풀 생성 완료: {} 개", eventId, eventSectors.size());
    }

    private void createThreadPoolForSector(Long sectorId) {
        if (sectorExecutors.containsKey(sectorId)) {
            log.info("Sector {} 스레드풀이 이미 존재합니다.", sectorId);
            return;
        }

        // 구간별 전용 큐 생성
        BlockingQueue<Runnable> sectorQueue = new LinkedBlockingQueue<>();
        sectorQueues.put(sectorId, sectorQueue);

        // 구간별 전용 스레드풀 생성 (단일 스레드로 순차 처리 보장)
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .threadNamePrefix("sector-" + sectorId + "-worker-%d")
                .build();

        int threadPoolSize = 1;
        ExecutorService executor = new ThreadPoolExecutor(
                threadPoolSize,
                threadPoolSize,
                0L,
                TimeUnit.MILLISECONDS,
                sectorQueue,
                threadFactory
        );

        sectorExecutors.put(sectorId, executor);
        log.info("Sector {} 전용 스레드풀 생성 완료", sectorId);
    }

    /**
     * 특정 구간의 스레드풀에 작업 제출
     */
    public void submitToSector(Long sectorId, Runnable task) {
        ExecutorService executor = sectorExecutors.get(sectorId);

        if (executor == null) {
            log.error("Sector {} 스레드풀이 존재하지 않습니다.", sectorId);
            // 동적 생성 (비상용)
            createThreadPoolForSector(sectorId);
            executor = sectorExecutors.get(sectorId);
        }

        executor.submit(task);
    }

    /**
     * Event 종료 시 해당 Event의 모든 Sector 스레드풀 정리
     */
    public void shutdownSectorThreadPools(Long eventId) {
        List<Sector> eventSectors = sectorAdaptor.findByEventId(eventId);

        for (Sector sector : eventSectors) {
            shutdownSectorThreadPool(sector.getId());
        }

        log.info("Event {} - 모든 구간 스레드풀 종료 완료", eventId);
    }

    private void shutdownSectorThreadPool(Long sectorId) {
        ExecutorService executor = sectorExecutors.remove(sectorId);
        sectorQueues.remove(sectorId);

        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
                log.info("Sector {} 스레드풀 종료 완료", sectorId);
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
                log.warn("Sector {} 스레드풀 강제 종료", sectorId);
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("모든 Sector 스레드풀 종료 시작...");

        sectorExecutors.forEach((sectorId, executor) -> {
            executor.shutdown();
        });

        sectorExecutors.forEach((sectorId, executor) -> {
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        });

        sectorExecutors.clear();
        sectorQueues.clear();
        log.info("모든 Sector 스레드풀 종료 완료");
    }
}
