package com.jnu.ticketdomain.AnnounceImage.config;


import javax.sql.DataSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.Properties;

@TestConfiguration
public class TestDataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource() {
        Properties properties = new Properties();
        properties.setProperty("useSSL", "false");
        properties.setProperty("serverTimezone", "Asia/Seoul");
        properties.setProperty("characterEncoding", "UTF-8");
        properties.setProperty("allowPublicKeyRetrieval", "true");
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://localhost:3306/ticket");
        dataSource.setUsername("root");
        dataSource.setPassword("");
        dataSource.setCatalog("ticket");
        dataSource.setConnectionProperties(properties);
        return dataSource;
    }

}
