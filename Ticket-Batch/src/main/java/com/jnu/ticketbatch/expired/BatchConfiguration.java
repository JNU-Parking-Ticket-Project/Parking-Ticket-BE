package com.jnu.ticketbatch.expired;


import com.jnu.ticketbatch.writer.RegistrationWriter;
import com.jnu.ticketdomain.common.domainEvent.Events;
import com.jnu.ticketdomain.domains.events.event.EventExpiredEvent;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.HibernateCursorItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.HibernateCursorItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
@Slf4j
public class BatchConfiguration {
    @Autowired private DataSource dataSource;
    @Autowired private SessionFactory sessionFactory;

    @Autowired private JobBuilderFactory jobBuilderFactory;

    @Autowired private StepBuilderFactory stepBuilderFactory;
    @Autowired private SampleChunkListener sampleChunkListener;

    @Bean
    public Job expirationJob() throws Exception {
        log.info(">>>>> expiredEventJob 살행");
        return jobBuilderFactory
                .get("EVENT_EXPIRED_JOB")
                .incrementer(new RunIdIncrementer())
                .start(processEventStep())
                .build();
    }

    @Bean
    @JobScope
    public Step processEventStep() throws Exception {
        return stepBuilderFactory
                .get("expiredEventStep")
                .<Registration, Registration>chunk(100)
                //            .reader(new ExpiredItemReader())
                .reader(customItemReader(null))
                .writer(jdbcPagingItemWriter())
                .listener(sampleChunkListener)
                .build();
    }

    @Bean
    @StepScope
    public HibernateCursorItemReader<Registration> customItemReader(
            @Value("#{jobParameters['eventId']}") Long eventId) {
        Events.raise(new EventExpiredEvent(eventId));
        return new HibernateCursorItemReaderBuilder<Registration>()
                .name("hibernateCursorReader")
                .sessionFactory(sessionFactory)
                .useStatelessSession(false)
                .queryString("from Registration r where r.sector.event.id = :eventId")
                .parameterValues(Collections.singletonMap("eventId", eventId))
                .build();
    }
    /**
     * ex sql SELECT r.id, r.sector_id, r.is_saved FROM registration_tb r JOIN sector s ON
     * r.sector_id = s.sector_id JOIN event e ON s.event_id = e.event_id WHERE e.event_id = 38;
     */
    private PagingQueryProvider createQueryProvider(Long eventId) throws Exception {
        SqlPagingQueryProviderFactoryBean queryProvider = new SqlPagingQueryProviderFactoryBean();
        queryProvider.setDataSource(dataSource);
        queryProvider.setSelectClause(
                "r.id, r.affiliation, r.car_num, r.created_at, r.email, r.is_deleted, r.is_light, r.is_saved, r.student_name, r.phone_num, r.student_num, r.sector_id, r.user_id");

        queryProvider.setFromClause(
                "from registration_tb r"
                        + " join sector s on r.sector_id = s.sector_id"
                        + " join event e on s.event_id = e.event_id"
                        + " where e.event_id = "
                        + 58);

        Map<String, Order> sortKeys = new HashMap<>();
        sortKeys.put("id", Order.ASCENDING);

        queryProvider.setSortKeys(sortKeys);

        return queryProvider.getObject();
    }

    @Bean
    public ItemWriter<? super Registration> jdbcPagingItemWriter() {
        return new RegistrationWriter(dataSource);
    }
}
