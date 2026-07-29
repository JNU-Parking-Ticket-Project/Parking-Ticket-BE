package com.jnu.ticketinfrastructure.service;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_CHANNEL;
import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_ISSUE_STREAM;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.registration.exception.AlreadyExistRegistrationException;
import com.jnu.ticketinfrastructure.model.AutoClaimResult;
import com.jnu.ticketinfrastructure.model.ChatMessage;
import com.jnu.ticketinfrastructure.model.RawStreamMessage;
import com.jnu.ticketinfrastructure.model.SectorStockInitialization;
import com.jnu.ticketinfrastructure.model.StockReservationResult;
import com.jnu.ticketinfrastructure.model.StreamConsumerState;
import com.jnu.ticketinfrastructure.model.StreamQueueMessage;
import com.jnu.ticketinfrastructure.redis.RedisRepository;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ConditionalOnExpression("${ableRedis:true}")
public class WaitingQueueService {

    private static final Logger tracker = LoggerFactory.getLogger("processTracker");
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
        ChatMessage message = new ChatMessage(registrationString, userId, sectorId, eventId);
        redisRepository.xAdd(key, message);
        tracker.info("Added to the stream, score:{}", score);
    }

    public StockReservationResult reserveAndRegisterQueue(
            String key, Registration registration, Long userId, Sector sector, Long eventId)
            throws JsonProcessingException {
        String registrationString = convertRegistrationJSON(registration);
        StockReservationResult result =
                redisRepository.reserveStockAndAddToStream(
                        stockKey(eventId, sector.getId()),
                        sequenceKey(eventId, sector.getId()),
                        reservedEmailKey(eventId),
                        key,
                        closedKey(eventId),
                        initializedKey(eventId),
                        registrationString,
                        userId,
                        sector.getId(),
                        eventId,
                        registration.getEmail(),
                        sector.getInitSectorCapacity());
        tracker.info(
                "Reserved Redis stock. sectorId: {}, reserved: {}, position: {}, status: {}, remaining: {}",
                sector.getId(),
                result.isReserved(),
                result.getPosition(),
                result.getResultStatus(),
                result.getRemainingAmount());
        return result;
    }

    public boolean initializeEventStock(Long eventId, List<Sector> sectors) {
        List<SectorStockInitialization> initializations =
                sectors.stream()
                        .map(sector -> toStockInitialization(eventId, sector))
                        .toList();
        return redisRepository.initializeEventStock(
                initializedKey(eventId),
                reservedEmailKey(eventId),
                closedKey(eventId),
                initializations);
    }

    public String convertRegistrationJSON(Registration registration) {
        JSONObject registrationJson = new JSONObject();
        registrationJson.put("email", registration.getEmail());
        registrationJson.put("name", registration.getName());
        registrationJson.put("studentNum", registration.getStudentNum());
        registrationJson.put("affiliation", registration.getAffiliation());
        registrationJson.put("department", registration.getDepartment());
        registrationJson.put("carNum", registration.getCarNum());
        registrationJson.put("phoneNum", registration.getPhoneNum());
        registrationJson.put("isDeleted", registration.isDeleted());
        registrationJson.put("isLight", registration.isLight());
        registrationJson.put("isSaved", registration.isSaved());
        registrationJson.put("savedAt", registration.getSavedAt());
        registrationJson.put("id", registration.getId());
        registrationJson.put("createdAt", registration.getCreatedAt());
        registrationJson.put("eventId", registration.getEventId());
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

    public void checkDuplicateData(String key, Object value) {
        Long rank = redisRepository.zRank(key, value);
        if (rank != null) {
            throw AlreadyExistRegistrationException.EXCEPTION;
        }
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

    public List<RawStreamMessage> readNewMessages(
            String key, String group, String consumer, long count, Duration blockTimeout) {
        return redisRepository.xReadGroupBlocking(key, group, consumer, count, blockTimeout);
    }

    public AutoClaimResult autoClaimMessages(
            String key,
            String group,
            String consumer,
            long count,
            Duration minIdleTime,
            String startId) {
        return redisRepository.xAutoClaim(key, group, consumer, count, minIdleTime, startId);
    }

    public StreamQueueMessage deserialize(String streamKey, RawStreamMessage rawMessage) {
        try {
            return new StreamQueueMessage(
                    streamKey,
                    rawMessage.getRecordId(),
                    objectMapper.readValue(rawMessage.getPayload(), ChatMessage.class));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Redis Stream message", e);
        }
    }

    public StreamConsumerState getConsumerState(String key, String group, int inFlight) {
        long pending = redisRepository.xPendingCount(key, group);
        long lag = Math.max(0L, redisRepository.xLength(key) - pending);
        return new StreamConsumerState(lag, pending, inFlight);
    }

    public Long acknowledge(String key, String group, String recordId) {
        return redisRepository.xAck(key, group, recordId);
    }

    public Long acknowledgeAndDelete(String key, String group, String recordId) {
        Long acknowledged = redisRepository.xAck(key, group, recordId);
        if (acknowledged != null && acknowledged > 0) {
            redisRepository.xDelete(key, recordId);
        }
        return acknowledged;
    }

    public String eventStreamKey(Long eventId) {
        return REDIS_EVENT_ISSUE_STREAM + ":{" + eventId + "}";
    }

    public void deleteEventStream(Long eventId) {
        redisRepository.delete(eventStreamKey(eventId));
    }

    public Optional<Integer> findRemainingStock(Long eventId, Long sectorId) {
        return redisRepository.getIntegerValue(stockKey(eventId, sectorId));
    }

    public void deleteEventStockKeys(Long eventId) {
        redisRepository.deleteKeysByPrefix(eventStockPrefix(eventId));
    }

    public void expireEventStockKeys(Long eventId, Duration timeout) {
        redisRepository.expireKeysByPrefix(eventStockPrefix(eventId), timeout);
    }

    public void markEventStockClosed(Long eventId, Duration timeout) {
        redisRepository.set(closedKey(eventId), true, timeout);
    }

    public String eventStockPrefix(Long eventId) {
        return "parking-ticket:event:{" + eventId + "}:";
    }

    public String stockKey(Long eventId, Long sectorId) {
        return eventStockPrefix(eventId) + "sector:" + sectorId + ":stock";
    }

    public String sequenceKey(Long eventId, Long sectorId) {
        return eventStockPrefix(eventId) + "sector:" + sectorId + ":sequence";
    }

    public String reservedEmailKey(Long eventId) {
        return eventStockPrefix(eventId) + "reserved:email";
    }

    public String closedKey(Long eventId) {
        return eventStockPrefix(eventId) + "closed";
    }

    public String initializedKey(Long eventId) {
        return eventStockPrefix(eventId) + "initialized";
    }

    private SectorStockInitialization toStockInitialization(Long eventId, Sector sector) {
        int issueAmount = sector.getIssueAmount();
        int remainingAmount =
                Math.max(
                        0,
                        Math.min(
                                Optional.ofNullable(sector.getRemainingAmount())
                                        .orElse(issueAmount),
                                issueAmount));
        return new SectorStockInitialization(
                stockKey(eventId, sector.getId()),
                sequenceKey(eventId, sector.getId()),
                remainingAmount,
                issueAmount - remainingAmount);
    }
}
