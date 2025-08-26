package com.jnu.ticketinfrastructure.service;

import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketinfrastructure.redis.RedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class RedisStreamRegistrationBroker  {

    public static final String STREAM_KEY_SECTOR = "stream-key-sector-";

    private final RedisRepository redisRepository;

    public void send(Registration registration, Long userId, Long sectorId, Long eventId) {
        String streamKey = STREAM_KEY_SECTOR + sectorId;
        Map<String, String> content = createRegistrationContent(registration, userId, sectorId, eventId);
        redisRepository.streamAdd(streamKey, content);
    }

    public List<MapRecord<String, String, String>> read(Long sectorId, String lastId, int count) {
        return redisRepository.streamRead(STREAM_KEY_SECTOR + sectorId, lastId, count);
    }

    private Map<String, String> createRegistrationContent(Registration registration, Long userId, Long sectorId, Long eventId) {
        Map<String, String> content = new HashMap<>();
        content.put("userId", String.valueOf(userId));
        content.put("eventId", String.valueOf(eventId));
        content.put("sectorId", String.valueOf(sectorId));
        content.put("email", registration.getEmail());
        content.put("name", registration.getName());
        content.put("studentNum", registration.getStudentNum());
        content.put("affiliation", registration.getAffiliation());
        content.put("department", registration.getDepartment());
        content.put("carNum", registration.getCarNum());
        content.put("phoneNum", registration.getPhoneNum());
        content.put("isDeleted", String.valueOf(registration.isDeleted()));
        content.put("isLight", String.valueOf(registration.isLight()));
        content.put("isSaved", String.valueOf(registration.isSaved()));
        content.put("savedAt", String.valueOf(registration.getSavedAt()));
        Long id = registration.getId();
        if (id != null) {
            content.put("id", String.valueOf(id));
        }
        LocalDateTime createdAt = registration.getCreatedAt();
        if (createdAt != null) {
            content.put("createdAt", createdAt.toString());
        }
        return content;
    }
}
