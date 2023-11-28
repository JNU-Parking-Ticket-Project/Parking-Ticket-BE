package com.jnu.ticketinfrastructure.config.aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesAsyncClient;

/**
 * AWS SES 연결하기 위한 설정 파일
 * accessKey, secretKey, region 을 application.yml 파일에 작성해야 한다.
 *
 * @author cookie
 * @version 1.0
 */
@Configuration
public class AwsSesConfig {

    @Value("${aws.ses.access-key}")
    private String accessKey;

    @Value("${aws.ses.secret-key}")
    private String secretKey;

    @Value("${aws.ses.region}")
    private String region;

    @Bean
    public SesAsyncClient sesAsyncClient() {

        return SesAsyncClient.builder()
                .credentialsProvider(() -> AwsBasicCredentials.create(accessKey, secretKey))
                .region(Region.of(region))
                .build();
    }

}