package com.jnu.ticketapi;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.jnu.ticket*")
public class TicketApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketApiApplication.class, args);
    }
}
