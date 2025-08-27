package com.jnu.ticketbatch.flow;

import com.jnu.ticketinfrastructure.model.RegistrationInFoRecord;
import com.jnu.ticketinfrastructure.model.RegistrationInfo;
import com.jnu.ticketinfrastructure.service.RedisStreamRegistrationBroker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class DataProcessor {

    private static final int DATA_BATCH_SIZE = 10;

    private final RedisStreamRegistrationBroker broker;
    private final RegistrationProcessor registrationProcessor;

    public void start(Long sectorId) {
        RecordId recordId = RecordId.of("0-0");

        while (true) {
            List<RegistrationInFoRecord> records = broker.readAfterId(sectorId, recordId, DATA_BATCH_SIZE);
            for (RegistrationInFoRecord record : records) {
                RegistrationInfo registrationInfo = record.registrationInfo();
                registrationProcessor.process(registrationInfo);
                recordId = record.recordId();
            }
        }
    }


}
