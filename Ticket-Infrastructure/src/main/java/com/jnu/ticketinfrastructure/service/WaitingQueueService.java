package com.jnu.ticketinfrastructure.service;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_CHANNEL;
import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_ISSUE_STREAM;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.events.exception.NotFoundSectorException;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.registration.exception.AlreadyExistRegistrationException;
import com.jnu.ticketinfrastructure.model.AutoClaimResult;
import com.jnu.ticketinfrastructure.model.ChatMessage;
import com.jnu.ticketinfrastructure.model.DeadLetterQueueMessage;
import com.jnu.ticketinfrastructure.model.DeadLetterTransferResult;
import com.jnu.ticketinfrastructure.model.RawStreamMessage;
import com.jnu.ticketinfrastructure.model.RegistrationPayloadConverter;
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
    private static final Duration DEAD_LETTER_RETENTION = Duration.ofDays(7);
    private static final Duration DRAINED_STREAM_RETENTION = Duration.ofDays(1);
    private static final long DEAD_LETTER_MAX_LENGTH = 1_000L;
    private static final int MAX_ERROR_LENGTH = 1_000;
    private static final String PROCESSING_FAILURE_REASON = "PROCESSING_FAILURE";
    private static final String EVENT_DRAIN_REASON = "EVENT_DRAIN_TIMEOUT";
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
            String key,
            String registrationPayload,
            String email,
            Long userId,
            Sector sector,
            Long eventId,
            Long journalId,
            Long admissionEpoch)
            throws JsonProcessingException {
        StockReservationResult result =
                redisRepository.reserveStockAndAddToStream(
                        stockKey(eventId, sector.getId()),
                        sequenceKey(eventId, sector.getId()),
                        reservedEmailKey(eventId),
                        key,
                        closedKey(eventId),
                        initializedKey(eventId),
                        decisionKey(eventId),
                        registrationPayload,
                        userId,
                        sector.getId(),
                        eventId,
                        email,
                        sector.getInitSectorCapacity(),
                        sector.getIssueAmount(),
                        journalId,
                        admissionEpoch);
        tracker.info(
                "Reserved Redis stock. sectorId: {}, reserved: {}, position: {}, status: {},"
                        + " remaining: {}",
                sector.getId(),
                result.isReserved(),
                result.getPosition(),
                result.getResultStatus(),
                result.getRemainingAmount());
        return result;
    }

    public boolean initializeEventStock(Long eventId, List<Sector> sectors) {
        if (sectors == null || sectors.isEmpty()) {
            throw NotFoundSectorException.EXCEPTION;
        }
        List<SectorStockInitialization> initializations =
                sectors.stream().map(sector -> toStockInitialization(eventId, sector)).toList();
        return redisRepository.initializeEventStock(
                initializedKey(eventId),
                reservedEmailKey(eventId),
                closedKey(eventId),
                initializations);
    }

    public boolean rebuildEventStock(
            Long eventId, List<Sector> sectors, Set<String> reservedEmails) {
        if (sectors == null || sectors.isEmpty()) {
            throw NotFoundSectorException.EXCEPTION;
        }
        List<SectorStockInitialization> initializations =
                sectors.stream().map(sector -> toStockInitialization(eventId, sector)).toList();
        return redisRepository.rebuildEventStock(
                initializedKey(eventId),
                reservedEmailKey(eventId),
                closedKey(eventId),
                initializations,
                reservedEmails == null ? Set.of() : reservedEmails);
    }

    public boolean isAvailable() {
        return redisRepository.ping();
    }

    public boolean isEventStockInitialized(Long eventId) {
        return redisRepository.hasKey(initializedKey(eventId));
    }

    public String convertRegistrationJSON(Registration registration) {
        return RegistrationPayloadConverter.toJson(registration);
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

    public boolean hasEventStreamMessages(Long eventId) {
        return redisRepository.xLength(eventStreamKey(eventId)) > 0L;
    }

    public Long acknowledge(String key, String group, String recordId) {
        return redisRepository.xAck(key, group, recordId);
    }

    public Long acknowledgeAndDelete(String key, String group, String recordId) {
        return redisRepository.xAcknowledgeAndDelete(key, streamFailureKey(key), group, recordId);
    }

    public DeadLetterTransferResult recordProcessingFailure(
            String streamKey,
            String group,
            String recordId,
            String payload,
            int maxFailures,
            Throwable throwable) {
        if (maxFailures < 1) {
            throw new IllegalArgumentException("maxFailures must be greater than zero");
        }
        return moveToDeadLetter(
                streamKey,
                group,
                recordId,
                payload,
                maxFailures,
                errorMessage(throwable),
                PROCESSING_FAILURE_REASON,
                false);
    }

    public long drainEventStream(Long eventId, String group) {
        String streamKey = eventStreamKey(eventId);
        List<RawStreamMessage> messages = redisRepository.xRangeRaw(streamKey);
        long movedCount = 0L;
        for (RawStreamMessage message : messages) {
            DeadLetterTransferResult result =
                    moveToDeadLetter(
                            streamKey,
                            group,
                            message.getRecordId(),
                            message.getPayload(),
                            1,
                            "Event queue drain grace period elapsed",
                            EVENT_DRAIN_REASON,
                            true);
            if (result.isMoved()) {
                movedCount++;
            }
        }
        redisRepository.expire(streamKey, DRAINED_STREAM_RETENTION);
        redisRepository.expire(streamFailureKey(streamKey), DRAINED_STREAM_RETENTION);
        return movedCount;
    }

    public List<DeadLetterQueueMessage> findDeadLetters(Long eventId, long count) {
        if (count < 1) {
            return List.of();
        }
        return redisRepository.xRangeDeadLetters(
                eventDeadLetterStreamKey(eventId), Math.min(count, DEAD_LETTER_MAX_LENGTH));
    }

    public boolean replayDeadLetter(Long eventId, String deadLetterRecordId) {
        String streamKey = eventStreamKey(eventId);
        Long replayed =
                redisRepository.xReplayDeadLetter(
                        eventDeadLetterStreamKey(eventId),
                        streamKey,
                        streamFailureKey(streamKey),
                        deadLetterRecordId);
        return replayed != null && replayed == 1L;
    }

    public String eventStreamKey(Long eventId) {
        return REDIS_EVENT_ISSUE_STREAM + ":{" + eventId + "}";
    }

    public String eventDeadLetterStreamKey(Long eventId) {
        return eventStreamKey(eventId) + ":dlq";
    }

    public void deleteEventStream(Long eventId) {
        redisRepository.delete(eventStreamKey(eventId));
    }

    public Optional<Integer> findRemainingStock(Long eventId, Long sectorId) {
        return redisRepository.getIntegerValue(stockKey(eventId, sectorId));
    }

    public Optional<Integer> findAssignedPosition(Long eventId, Long sectorId) {
        return redisRepository.getIntegerValue(sequenceKey(eventId, sectorId));
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

    public String decisionKey(Long eventId) {
        return eventStockPrefix(eventId) + "decision:journal";
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

    private DeadLetterTransferResult moveToDeadLetter(
            String streamKey,
            String group,
            String recordId,
            String payload,
            int maxFailures,
            String lastError,
            String reason,
            boolean forceMove) {
        return redisRepository.xRecordFailureAndMaybeMoveToDeadLetter(
                streamKey,
                streamFailureKey(streamKey),
                streamDeadLetterKey(streamKey),
                group,
                recordId,
                payload,
                maxFailures,
                lastError,
                System.currentTimeMillis(),
                reason,
                DEAD_LETTER_MAX_LENGTH,
                DEAD_LETTER_RETENTION,
                forceMove);
    }

    private String streamFailureKey(String streamKey) {
        return streamKey + ":failures";
    }

    private String streamDeadLetterKey(String streamKey) {
        return streamKey + ":dlq";
    }

    private String errorMessage(Throwable throwable) {
        String message = throwable.getMessage();
        String error = throwable.getClass().getName() + (message == null ? "" : ": " + message);
        return error.substring(0, Math.min(error.length(), MAX_ERROR_LENGTH));
    }
}
