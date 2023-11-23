package com.jnu.ticketdomain;


import com.jnu.ticketcommon.TicketCommonApplication;
import com.jnu.ticketinfrastructure.TicketInfrastructureApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/** 스프링 부트 설정의 컴포넌트 스캔범위를 지정 통합 테스트를 위함 */
@Configuration
@ComponentScan(
        basePackageClasses = {
            TicketInfrastructureApplication.class,
            TicketDomainApplication.class,
            TicketCommonApplication.class
        })
public class DomainIntegrateTestConfig {}
