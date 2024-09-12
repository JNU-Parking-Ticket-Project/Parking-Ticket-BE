package com.jnu.ticketbatch.config;


import javax.sql.DataSource;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

@Configuration
public class QuartzConfig {

    @Autowired private AutoWiringSpringBeanJobFactory jobFactory;
    @Autowired private DataSource dataSource;

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() throws SchedulerException {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setJobFactory(jobFactory); // AutoWiringSpringBeanJobFactory 설정
        factory.setDataSource(dataSource); // DataSource 설정
        return factory;
    }
}
