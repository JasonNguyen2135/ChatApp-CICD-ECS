package com.project.chatapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.chatapp.model.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisSubscriber {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public void onMessage(String message) {
        try {
            // Khi nhận được message từ Redis, đẩy ra WebSocket cho các client tương ứng
            ChatMessage chatMessage = objectMapper.readValue(message, ChatMessage.class);
            messagingTemplate.convertAndSend("/topic/messages/" + chatMessage.getReceiverId(), chatMessage);
            messagingTemplate.convertAndSend("/topic/messages/" + chatMessage.getSenderId(), chatMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
