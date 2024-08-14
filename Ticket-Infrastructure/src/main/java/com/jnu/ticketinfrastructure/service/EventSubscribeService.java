package com.jnu.ticketinfrastructure.service;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_CHANNEL;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketinfrastructure.domainEvent.EventIssuedEvent;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventSubscribeService implements MessageListener {
    @Autowired @Lazy private RedisTemplate<String, Object> redisTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        Long userId = null;
        Long sectorId = null;
        Long eventId = null;
        Registration registration = null;

        try {
            JSONObject jsonObj = new JSONObject(new String(message.getBody()));
            userId = Long.valueOf(jsonObj.get("userId").toString());
            sectorId = Long.valueOf(jsonObj.get("sectorId").toString());
            registration = convertFromString(jsonObj.get("registration").toString());
            eventId = Long.valueOf(jsonObj.get("eventId").toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        // "userId" 키의 값을 추출
        if (REDIS_EVENT_CHANNEL.equals(channel)) {
            handleReceivedUserId(sectorId, userId, eventId);
        } else {
            log.error("Received message from unknown channel: {}", channel);
        }
    }

    public void handleReceivedUserId(Long sectorId, Long userId, Long eventId) {
        eventPublisher.publishEvent(EventIssuedEvent.from(sectorId, userId, eventId));
    }

    public Registration convertFromString(String jsonString) {
        // JSON 문자열을 Map으로 변환
        JSONObject jsonObj = new JSONObject(jsonString);
        String email = jsonObj.get("email").toString();
        String name = jsonObj.get("name").toString();
        String studentNum = jsonObj.get("studentNum").toString();
        String affiliation = jsonObj.get("affiliation").toString();
        String carNum = jsonObj.get("carNum").toString();
        String phoneNum = jsonObj.get("phoneNum").toString();
        // createdAt은 null일 수 있으므로 별도 처리가 필요합니다.
        boolean isLight = Boolean.parseBoolean(jsonObj.get("isLight").toString());
        boolean isSaved = Boolean.parseBoolean(jsonObj.get("isSaved").toString());
        // Now you can create a Registration object using the extracted values
        return Registration.builder()
                .email(email)
                .name(name)
                .studentNum(studentNum)
                .affiliation(affiliation)
                .carNum(carNum)
                .isLight(isLight)
                .phoneNum(phoneNum)
                .createdAt(LocalDateTime.now())
                .isSaved(isSaved)
                .build();
    }
}
