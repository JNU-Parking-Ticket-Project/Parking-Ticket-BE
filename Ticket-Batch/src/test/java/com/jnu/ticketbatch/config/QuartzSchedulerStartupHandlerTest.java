package com.jnu.ticketbatch.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.Scheduler;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class QuartzSchedulerStartupHandlerTest {

    @Mock private Scheduler scheduler;
    @Mock private AutoWiringSpringBeanJobFactory jobFactory;
    @Mock private DataSource dataSource;

    @Test
    @DisplayName("Quartz는 Spring context 생성 중 자동 시작하지 않는다")
    void disablesQuartzAutoStartup() throws Exception {
        QuartzConfig quartzConfig = new QuartzConfig();
        ReflectionTestUtils.setField(quartzConfig, "jobFactory", jobFactory);
        ReflectionTestUtils.setField(quartzConfig, "dataSource", dataSource);

        SchedulerFactoryBean factory = quartzConfig.schedulerFactoryBean();

        assertThat(factory.isAutoStartup()).isFalse();
    }

    @Test
    @DisplayName("ApplicationReadyEvent의 Stream 복원 단계 이후 Quartz를 시작한다")
    void startsQuartzAfterApplicationIsReady() throws Exception {
        QuartzSchedulerStartupHandler handler = new QuartzSchedulerStartupHandler(scheduler);

        handler.startScheduler();

        verify(scheduler).start();
    }
}
