package com.jnu.ticketapi;


import com.jnu.ticketapi.config.WebProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties({
    WebProperties.class,
})
@Configuration
public class ConfigurationApiPropertiesConfig {}
