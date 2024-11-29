package com.jnu.ticketapi.config;


import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@Getter
@AllArgsConstructor
@ConstructorBinding
@ConfigurationProperties(prefix = "encryption")
public class EncryptionProperties {
    private String key;
    private String algorithm;
    private String keySpecAlgorithm;
    private Long length;
}
