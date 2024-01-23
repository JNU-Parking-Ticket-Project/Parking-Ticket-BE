package com.jnu.ticketbatch.writer;


import com.jnu.ticketdomain.domains.registration.domain.Registration;
import java.util.List;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@StepScope
@Slf4j
public class RegistrationWriter implements ItemWriter<Registration> {

    private final Logger logger = LoggerFactory.getLogger(RegistrationWriter.class);
    private JdbcBatchItemWriter<Registration> delegate;
    private DataSource dataSource;
    private static final String sql = "update registration_tb set is_saved = false where id = :id";

    public RegistrationWriter() {}

    public RegistrationWriter(DataSource dataSource) {
        if (dataSource == null) logger.info("dataSource is null");
        else logger.info("dataSource is injected : " + dataSource.toString());
        this.dataSource = dataSource;
        this.delegate = new JdbcBatchItemWriter<Registration>();
        this.delegate.setItemSqlParameterSourceProvider(
                new BeanPropertyItemSqlParameterSourceProvider<Registration>());
        this.delegate.setDataSource(dataSource);
        this.delegate.setJdbcTemplate(new NamedParameterJdbcTemplate(dataSource));
        this.delegate.setSql(sql);
        this.delegate.afterPropertiesSet();
    }

    @Override
    public void write(List<? extends Registration> items) throws Exception {
        logger.info("[RegistrationJob] : ItemWriter started. Items size: " + items.size());
        for (Registration item : items) {
            logger.info("[RegistrationJob] : Item - " + item.toString());
        }
        this.delegate.write(items);
    }

    public JdbcBatchItemWriter<Registration> getDelegate() {
        return this.delegate;
    }
}
