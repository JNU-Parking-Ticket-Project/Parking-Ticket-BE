package com.jnu.ticketinfrastructure;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;

/** 도메인 모듈의 통합테스트의 편의성을 위해서 만든 어노테이션 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(classes = InfraIntegrateTestConfig.class)
@Documented
public @interface InfraIntegrateSpringBootTest {}
