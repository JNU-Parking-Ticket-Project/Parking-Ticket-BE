package com.jnu.ticketapi.config;


import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.jnu.ticketinfrastructure.redis.RedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_ISSUE_STORE;

@Component
@RequiredArgsConstructor
public class DatabaseCleaner {

    private final List<String> tableNames = new ArrayList<>();

    @PersistenceContext private EntityManager entityManager;
    private final RedisRepository redisRepository;

    // 바뀐부분 : 의존성 주입이후 초기화 수행 시 Table을 조회한다.
    @PostConstruct
    @SuppressWarnings("unchecked")
    private void findDatabaseTableNames() {
        List<Object[]> tableInfos = entityManager.createNativeQuery("SHOW TABLES").getResultList();
        for (Object[] tableInfo : tableInfos) {
            String tableName = (String) tableInfo[0];
            tableNames.add(tableName);
        }
    }

    private void truncate() {
        entityManager
                .createNativeQuery(String.format("SET FOREIGN_KEY_CHECKS %d", 0))
                .executeUpdate();
        for (String tableName : tableNames) {
            entityManager
                    .createNativeQuery(String.format("TRUNCATE TABLE %s", tableName))
                    .executeUpdate();
        }
        entityManager
                .createNativeQuery(String.format("SET FOREIGN_KEY_CHECKS %d", 1))
                .executeUpdate();
    }

    private void deleteKey() {
        redisRepository.delete(REDIS_EVENT_ISSUE_STORE);
    }

    @Transactional
    public void clear() {
        entityManager.clear();
        truncate();
        deleteKey();
    }
}
