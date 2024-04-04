package com.jnu.ticketinfrastructure;


import com.jnu.ticketinfrastructure.slack.config.SlackProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties({
    SlackProperties.class,
})
@Configuration
public class ConfigurationInfrastructurePropertiesConfig {}
