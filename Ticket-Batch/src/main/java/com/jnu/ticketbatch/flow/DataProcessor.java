package com.jnu.ticketbatch.flow;

import com.jnu.ticketinfrastructure.service.RedisStreamRegistrationBroker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

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
            List<MapRecord<String, String, String>> records = broker.readAfterId(sectorId, recordId, DATA_BATCH_SIZE);
            for (MapRecord<String, String, String> record : records) {

                Map<String, String> content = record.getValue();
                registrationProcessor.process(content);
                recordId = record.getId();
            }
        }
    }


}
