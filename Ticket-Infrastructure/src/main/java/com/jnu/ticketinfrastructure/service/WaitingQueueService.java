package com.jnu.ticketinfrastructure.service;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_CHANNEL;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketinfrastructure.model.ChatMessage;
import com.jnu.ticketinfrastructure.redis.RedisRepository;
import java.util.LinkedList;
import java.util.Queue;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WaitingQueueService {
    private final RedisRepository redisRepository;
    @Autowired private ObjectMapper objectMapper;

    public WaitingQueueService(RedisRepository redisRepository) {
        this.redisRepository = redisRepository;
    }

    public Boolean registerQueue(String key, Registration registration, Long userId, Long sectorId)
            throws JsonProcessingException {
        Double score = (double) System.currentTimeMillis();
        //        registration to JSON String
        //        String registrationString = registration.toString();
        String registrationString = convertRegistrationJSON(registration);
        //            String registrationString = objectMapper.writeValueAsString(registration);
        ChatMessage message = new ChatMessage(registrationString, userId, sectorId);
        boolean isPresent = redisRepository.zAddIfAbsent(key, message, score);
        // 재고가 있어야 처리
        if (isPresent) {
            publishMessage(message);
        }
        return true;
    }

    public String convertRegistrationJSON(Registration registration) {
        JSONObject registrationJson = new JSONObject();
        registrationJson.put("email", registration.getEmail());
        registrationJson.put("name", registration.getName());
        registrationJson.put("studentNum", registration.getStudentNum());
        registrationJson.put("affiliation", registration.getAffiliation());
        registrationJson.put("carNum", registration.getCarNum());
        registrationJson.put("phoneNum", registration.getPhoneNum());
//        registrationJson.put("createdAt", registration.getCreatedAt());
        registrationJson.put("deleted", registration.isDeleted());
        registrationJson.put("light", registration.isLight());
        registrationJson.put("saved", registration.isSaved());
        return registrationJson.toString();
    }

    private void publishMessage(ChatMessage message) {
        redisRepository.converAndSend(REDIS_EVENT_CHANNEL, message);
    }

    public <T> Queue<T> getQueue(String key, long startRank, long endRank, Class<T> type) {
        return (Queue<T>) new LinkedList<>(redisRepository.zRange(key, startRank, endRank, type));
    }

    public <T> Queue<T> popQueue(String key, long count, Class<T> type) {
        Queue<T> set = redisRepository.zPopMin(key, count, type);
        return new LinkedList<>(set);
    }

    public Object popValue(String key) {
        return redisRepository.zPopMin(key);
    }

    public Long getWaitingOrder(String key, Object value) {
        return redisRepository.zRank(key, value);
    }
}
