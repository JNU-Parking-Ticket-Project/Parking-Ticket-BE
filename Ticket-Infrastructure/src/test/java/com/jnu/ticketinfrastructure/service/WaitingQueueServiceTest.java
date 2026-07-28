package com.jnu.ticketinfrastructure.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketinfrastructure.model.ChatMessage;
import com.jnu.ticketinfrastructure.model.StreamQueueMessage;
import com.jnu.ticketinfrastructure.redis.RedisRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WaitingQueueServiceTest {

    private static final String STREAM_KEY = "event-issue-stream";

    @Mock private RedisRepository redisRepository;

    private WaitingQueueService waitingQueueService;

    @BeforeEach
    void setUp() {
        waitingQueueService = new WaitingQueueService(redisRepository);
    }

    @Test
    @DisplayName("신청 대기열 등록은 Redis Stream에 ChatMessage payload로 저장한다")
    void registerQueueAddsMessageToRedisStream() throws Exception {
        Registration registration = registration();

        waitingQueueService.registerQueue(STREAM_KEY, registration, 1L, 2L, 3L);

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(redisRepository).xAdd(eq(STREAM_KEY), messageCaptor.capture());
        verify(redisRepository, never()).zAddIfAbsent(anyString(), any(), anyDouble());

        ChatMessage message = messageCaptor.getValue();
        assertThat(message.getUserId()).isEqualTo(1L);
        assertThat(message.getSectorId()).isEqualTo(2L);
        assertThat(message.getEventId()).isEqualTo(3L);

        JSONObject payload = new JSONObject(message.getRegistration());
        assertThat(payload.getLong("id")).isEqualTo(10L);
        assertThat(payload.getString("email")).isEqualTo("student@jnu.ac.kr");
        assertThat(payload.getString("studentNum")).isEqualTo("20240001");
        assertThat(payload.getBoolean("isSaved")).isFalse();
        assertThat(payload.getLong("eventId")).isEqualTo(3L);
    }

    @Test
    @DisplayName("consumer group 조회는 Redis Stream readGroup에 위임한다")
    void readGroupDelegatesToRedisStreamRepository() {
        List<StreamQueueMessage> messages =
                List.of(new StreamQueueMessage("1-0", new ChatMessage("{}", 1L, 2L, 3L)));
        when(redisRepository.xClaimStale(
                        STREAM_KEY, "group", "consumer", 100L, Duration.ofSeconds(30)))
                .thenReturn(List.of());
        when(redisRepository.xReadGroup(STREAM_KEY, "group", "consumer", 100L))
                .thenReturn(messages);

        List<StreamQueueMessage> result =
                waitingQueueService.readGroup(STREAM_KEY, "group", "consumer", 100L);

        assertThat(result).containsExactlyElementsOf(messages);
    }

    @Test
    @DisplayName("stale pending record를 먼저 복구하고 남은 수만큼 새 메시지를 읽는다")
    void readGroupRecoversStalePendingBeforeNewMessages() {
        StreamQueueMessage recovered =
                new StreamQueueMessage("1-0", new ChatMessage("{}", 1L, 2L, 3L));
        StreamQueueMessage newMessage =
                new StreamQueueMessage("2-0", new ChatMessage("{}", 2L, 2L, 3L));
        when(redisRepository.xClaimStale(
                        STREAM_KEY, "group", "consumer", 2L, Duration.ofSeconds(30)))
                .thenReturn(List.of(recovered));
        when(redisRepository.xReadGroup(STREAM_KEY, "group", "consumer", 1L))
                .thenReturn(List.of(newMessage));

        List<StreamQueueMessage> result =
                waitingQueueService.readGroup(STREAM_KEY, "group", "consumer", 2L);

        assertThat(result).containsExactly(recovered, newMessage);
    }

    @Test
    @DisplayName("처리 완료된 Stream record는 acknowledge로 ACK 처리한다")
    void acknowledgeDelegatesToRedisStreamRepository() {
        when(redisRepository.xAck(STREAM_KEY, "group", "1-0")).thenReturn(1L);

        Long acknowledged = waitingQueueService.acknowledge(STREAM_KEY, "group", "1-0");

        assertThat(acknowledged).isEqualTo(1L);
    }

    @Test
    @DisplayName("ACK에 성공한 Stream record는 원본 entry도 삭제한다")
    void acknowledgeAndDeleteRemovesAcknowledgedRecord() {
        when(redisRepository.xAck(STREAM_KEY, "group", "1-0")).thenReturn(1L);

        Long acknowledged = waitingQueueService.acknowledgeAndDelete(STREAM_KEY, "group", "1-0");

        assertThat(acknowledged).isEqualTo(1L);
        verify(redisRepository).xDelete(STREAM_KEY, "1-0");
    }

    @Test
    @DisplayName("ACK하지 못한 Stream record는 삭제하지 않는다")
    void acknowledgeAndDeleteKeepsUnacknowledgedRecord() {
        when(redisRepository.xAck(STREAM_KEY, "group", "1-0")).thenReturn(0L);

        waitingQueueService.acknowledgeAndDelete(STREAM_KEY, "group", "1-0");

        verify(redisRepository, never()).xDelete(STREAM_KEY, "1-0");
    }

    @Test
    @DisplayName("이벤트별 Stream key는 Redis Cluster hash tag에 eventId를 사용한다")
    void eventStreamKeyUsesEventHashTag() {
        assertThat(waitingQueueService.eventStreamKey(3L)).isEqualTo("쿠폰 발급 스트림:{3}");
    }

    @Test
    @DisplayName("이벤트 삭제는 해당 이벤트의 Stream만 삭제한다")
    void deleteEventStreamDeletesOnlyEventStreamKey() {
        waitingQueueService.deleteEventStream(3L);

        verify(redisRepository).delete("쿠폰 발급 스트림:{3}");
    }

    private Registration registration() {
        Registration registration =
                Registration.builder()
                        .email("student@jnu.ac.kr")
                        .name("학생")
                        .studentNum("20240001")
                        .affiliation("공과대학")
                        .department("컴퓨터공학과")
                        .carNum("12가3456")
                        .isLight(false)
                        .phoneNum("010-0000-0000")
                        .createdAt(LocalDateTime.of(2026, 6, 25, 10, 0))
                        .isSaved(false)
                        .savedAt(null)
                        .eventId(3L)
                        .build();
        registration.setId(10L);
        return registration;
    }
}
