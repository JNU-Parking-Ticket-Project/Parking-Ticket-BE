package com.jnu.ticketdomain.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModuleConfig {
    @Bean
    public ObjectMapper objectMapper() {
        PolymorphicTypeValidator ptv =
                BasicPolymorphicTypeValidator.builder().allowIfSubType(Object.class).build();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(ptv, DefaultTyping.NON_FINAL);
        // LocalDatteTime 직렬화를 위한 설정
        return objectMapper;
    }
}
