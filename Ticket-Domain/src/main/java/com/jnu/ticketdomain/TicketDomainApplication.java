package com.jnu.ticketdomain;


import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Date;
import java.util.TimeZone;
import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class TicketDomainApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketDomainApplication.class, args);
    }

    @PostConstruct
    public void changeTimeKST() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        System.out.println("현재시각 : " + new Date());
    }

    @Bean
    JPAQueryFactory jpaQueryFactory(EntityManager em) {
        return new JPAQueryFactory(em);
    }
}
