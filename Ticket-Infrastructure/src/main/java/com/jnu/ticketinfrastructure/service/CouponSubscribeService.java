package com.jnu.ticketinfrastructure.service;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketinfrastructure.model.ChatMessage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponSubscribeService implements MessageListener {
    public static List<String> messageList = new ArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            Map<String, String> map = mapper.readValue(message.toString(), new TypeReference<Map<String, String>>() {});
            String chatMessage = map.get("message");
            log.info("chatMessage : {}", chatMessage);
            messageList.add(chatMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }
        messageList.add(message.toString());
    }
}
