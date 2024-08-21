package com.jnu.ticketinfrastructure.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.registration.exception.AlreadyExistRegistrationException;
import com.jnu.ticketinfrastructure.model.ChatMessage;
import com.jnu.ticketinfrastructure.model.ChatMessageStatus;
import com.jnu.ticketinfrastructure.redis.RedisRepository;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_CHANNEL;

@Service
@Slf4j
public class WaitingQueueService {
    private final RedisRepository redisRepository;
    @Autowired
    private ObjectMapper objectMapper;

    public WaitingQueueService(RedisRepository redisRepository) {
        this.redisRepository = redisRepository;
    }


    public void registerQueue(
            String key, Registration registration, Long userId, Long sectorId, Long eventId)
            throws JsonProcessingException {
        Double score = (double) System.currentTimeMillis();
        //        registration to JSON String
        //        String registrationString = registration.toString();
        String registrationString = convertRegistrationJSON(registration);
        //            String registrationString = objectMapper.writeValueAsString(registration);
        ChatMessage message = new ChatMessage(registrationString, userId, sectorId, eventId, ChatMessageStatus.NOT_WAITING.name());
        checkDuplicateData(key, message);
        redisRepository.zAddIfAbsent(key, message, score);
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
        registrationJson.put("isDeleted", registration.isDeleted());
        registrationJson.put("isLight", registration.isLight());
        registrationJson.put("isSaved", registration.isSaved());
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

    public ChatMessage getValueNotWaiting(String key) {
        // Get the first element in the ZSET (lowest score) without removing it
        Set<Object> resultSet = redisRepository.zRange(key, 0L, 0L, Object.class);
        log.info("resultSetSize: {}", resultSet.size());
        Set<ChatMessage> chatMessages = resultSet.stream().map(o -> (ChatMessage) o)
                .filter(chatMessage -> Objects.equals(chatMessage.getStatus(), ChatMessageStatus.NOT_WAITING.name()))
                .collect(Collectors.toSet());
        log.info("chatMessages: {}", chatMessages);
        if (chatMessages != null && !chatMessages.isEmpty()) {
            // Return the first element in the set
            return chatMessages.iterator().next();
        } else {
            // If the set is empty, return null or handle accordingly
            return null;
        }
    }

    public void checkDuplicateData(String key, Object value) {
        Long rank = redisRepository.zRank(key, value);
        if (rank != null) {
            throw AlreadyExistRegistrationException.EXCEPTION;
        }
    }

    public void reRegisterQueue(String key, ChatMessage message, ChatMessageStatus newStatus,  Double score) {
        log.info("redis에 데이터 다시 넣기");
        redisRepository.removeAndAdd(key, message, newStatus, score);
    }

    public Double getScore(String key, Object value) {
        return redisRepository.getScore(key, value);
    }

    public void remove(String key, Object value) {
        redisRepository.remove(key, value);
    }

    public Object getValue(String key) {
        // Get the first element in the ZSET (lowest score) without removing it
        Set<Object> resultSet = redisRepository.zRange(key, 0L, 0L, Object.class);
        log.info("resultSetSize: {}", resultSet.size());
        if (resultSet != null && !resultSet.isEmpty()) {
            // Return the first element in the set
            return resultSet.iterator().next();
        } else {
            // If the set is empty, return null or handle accordingly
            return null;
        }
    }
}
