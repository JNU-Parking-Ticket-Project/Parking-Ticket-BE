package com.jnu.ticketdomain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class TicketDomainApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketDomainApplication.class, args);
    }

}
