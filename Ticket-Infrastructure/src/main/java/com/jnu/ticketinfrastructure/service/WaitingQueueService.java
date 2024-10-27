package com.jnu.ticketinfrastructure.service;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_CHANNEL;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.registration.exception.AlreadyExistRegistrationException;
import com.jnu.ticketinfrastructure.model.ChatMessage;
import com.jnu.ticketinfrastructure.model.ChatMessageStatus;
import com.jnu.ticketinfrastructure.redis.RedisRepository;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ConditionalOnExpression("${ableRedis:true}")
public class WaitingQueueService {
    private final RedisRepository redisRepository;
    @Autowired private ObjectMapper objectMapper;

    public WaitingQueueService(RedisRepository redisRepository) {
        this.redisRepository = redisRepository;
    }

    public void registerQueue(
            String key, Registration registration, Long userId, Long sectorId, Long eventId)
            throws JsonProcessingException {
        Double score = (double) System.currentTimeMillis();
        String registrationString = convertRegistrationJSON(registration);
        ChatMessage message =
                new ChatMessage(
                        registrationString,
                        userId,
                        sectorId,
                        eventId,
                        ChatMessageStatus.NOT_WAITING.name());
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
        registrationJson.put("isDeleted", registration.isDeleted());
        registrationJson.put("isLight", registration.isLight());
        registrationJson.put("isSaved", registration.isSaved());
        registrationJson.put("savedAt", registration.getSavedAt());
        registrationJson.put("id", registration.getId());
        registrationJson.put("createdAt", registration.getCreatedAt());
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

    public ChatMessage findFirstByStatus(String key, ChatMessageStatus status) {
        Set<Object> resultSet = redisRepository.zRange(key, 0L, 0L, Object.class);
        Set<ChatMessage> chatMessages =
                resultSet.stream()
                        .map(o -> (ChatMessage) o)
                        .filter(
                                chatMessage ->
                                        Objects.equals(chatMessage.getStatus(), status.name()))
                        .collect(Collectors.toSet());
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

    public void reRegisterQueue(
            String key, ChatMessage message, ChatMessageStatus newStatus, Double score) {
        redisRepository.removeAndAdd(key, message, newStatus, score);
    }

    public Double getScore(String key, Object value) {
        return redisRepository.getScore(key, value);
    }

    public Long remove(String key, Object value) {
        return redisRepository.remove(key, value);
    }

    public Object findFirst(String key) {
        // Get the first element in the ZSET (lowest score) without removing it
        Set<Object> resultSet = redisRepository.zRange(key, 0L, 0L, Object.class);
        if (resultSet != null && !resultSet.isEmpty()) {
            // Return the first element in the set
            return resultSet.iterator().next();
        } else {
            // If the set is empty, return null or handle accordingly
            return null;
        }
    }

    public Set<ZSetOperations.TypedTuple<Object>> findAllWithScore(String key) {
        return redisRepository.zRangeWithScores(key, 0L, -1L);
    }
}
