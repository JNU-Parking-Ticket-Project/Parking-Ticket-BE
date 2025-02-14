package com.jnu.ticketdomain.AnnounceImage.config;


import javax.sql.DataSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

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
        dataSource.setPassword("1234");
//        dataSource.setCatalog("ticket");
        dataSource.setConnectionProperties(properties);
        return dataSource;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        // 패키지 스캔 경로를 실제 엔티티들이 위치한 패키지로 지정
        emf.setPackagesToScan("com.jnu.ticketdomain.domains");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        emf.setJpaVendorAdapter(vendorAdapter);

        Properties jpaProperties = new Properties();
        jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
        // 필요한 추가 JPA 속성들을 설정
        emf.setJpaProperties(jpaProperties);

        return emf;
    }
}
