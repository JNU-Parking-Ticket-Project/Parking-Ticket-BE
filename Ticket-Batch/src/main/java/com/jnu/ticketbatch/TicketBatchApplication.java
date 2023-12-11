package com.jnu.ticketbatch;


import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableBatchProcessing
@SpringBootApplication
public class TicketBatchApplication {
    public static void main(String[] args) {
        final var context = SpringApplication.run(TicketBatchApplication.class, args);
        System.exit(SpringApplication.exit(context));
    }
}
