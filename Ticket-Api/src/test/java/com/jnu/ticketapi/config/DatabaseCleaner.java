package com.jnu.ticketapi.config;


import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class DatabaseCleaner {

    private final List<String> tableNames = new ArrayList<>();

    @PersistenceContext
    private final EntityManager entityManager;
    private final DataSource dataSource;

    public DatabaseCleaner(DataSource dataSource, EntityManager entityManager) {
        this.dataSource = dataSource;
        this.entityManager = entityManager;
    }

    // 바뀐부분 : 의존성 주입이후 초기화 수행 시 Table을 조회한다.
    @PostConstruct
    @SuppressWarnings("unchecked")
    private void findDatabaseTableNames() throws SQLException {
        List<?> tableInfos = entityManager.createNativeQuery("SHOW TABLES").getResultList();
        String databaseVendor = getDatabaseVendor();
        if (databaseVendor.equals("MySQL")) {
            extractFromMysql(tableInfos);
        }

        if (databaseVendor.equals("H2")) {
            extractFromH2(tableInfos);
        }
    }

    private String getDatabaseVendor() throws SQLException {
        Connection connection = dataSource.getConnection();
        DatabaseMetaData metaData = connection.getMetaData();
        return metaData.getDatabaseProductName();
    }

    private void extractFromMysql(List<?> tableInfos) {
        Object tableInfo = tableInfos.get(0);
        String tableNames = (String) tableInfo;
        this.tableNames.addAll(Arrays.stream(tableNames.split(",")).toList());
    }

    private void extractFromH2(List<?> tableInfos) {
        for (Object tableInfo : tableInfos) {
            Object[] tableNames = (Object[]) tableInfo;
            this.tableNames.add((String) tableNames[0]);
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

    @Transactional
    public void clear() {
        entityManager.clear();
        truncate();
    }
}
