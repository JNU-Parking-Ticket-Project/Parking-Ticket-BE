package com.jnu.ticketinfrastructure.service;


import com.jnu.ticketinfrastructure.model.RegistrationInFoRecord;
import com.jnu.ticketinfrastructure.model.RegistrationInfo;
import com.jnu.ticketinfrastructure.redis.RedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisStreamRegistrationBroker {

    public static final String STREAM_KEY_SECTOR = "stream-key-sector-";

    private final RedisRepository redisRepository;

    public void send(RegistrationInfo registrationDto) {
        String streamKey = STREAM_KEY_SECTOR + registrationDto.getSectorId();
        redisRepository.streamAdd(streamKey, registrationDto);
    }

    public List<RegistrationInFoRecord> readAfterId(Long sectorId, RecordId id, int count) {
        return redisRepository.streamReadAfterId(STREAM_KEY_SECTOR + sectorId, id, count);
    }

}
