package com.project.chatapp.controller;

import com.project.chatapp.model.ChatMessage;
import com.project.chatapp.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatController {

    private final MessageRepository messageRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic topic;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        chatMessage.setId(null);
        if (chatMessage.getIsRevoked() == null) chatMessage.setIsRevoked(false);
        if (chatMessage.getIsPinned() == null) chatMessage.setIsPinned(false);
        if (chatMessage.getTimestamp() == null || chatMessage.getTimestamp() == 0) {
            chatMessage.setTimestamp(System.currentTimeMillis());
        }

        ChatMessage savedMsg = messageRepository.save(chatMessage);
        
        // Gửi qua Redis để tất cả các instance ECS đều nhận được và đẩy cho client
        redisTemplate.convertAndSend(topic.getTopic(), savedMsg);
    }

    @GetMapping("/api/messages/{senderId}/{receiverId}")
    public List<ChatMessage> getChatHistory(
            @PathVariable String senderId,
            @PathVariable String receiverId) {
        return messageRepository.findChatHistory(senderId, receiverId);
    }

    @MessageMapping("/chat.sendReaction")
    public void handleReaction(@Payload Map<String, Object> payload) {
        Long messageId = Long.valueOf(payload.get("id").toString());
        String reaction = (String) payload.get("reaction");

        ChatMessage msg = messageRepository.findById(messageId).orElse(null);
        if (msg != null) {
            msg.setReaction(reaction);
            messageRepository.save(msg);
            redisTemplate.convertAndSend(topic.getTopic(), msg);
        }
    }

    @MessageMapping("/chat.revokeMessage")
    public void revokeMessage(@Payload Map<String, Object> payload) {
        Long messageId = Long.valueOf(payload.get("id").toString());
        ChatMessage msg = messageRepository.findById(messageId).orElse(null);
        if (msg != null) {
            msg.setIsRevoked(true);
            msg.setText("Tin nhắn đã bị thu hồi");
            messageRepository.save(msg);
            redisTemplate.convertAndSend(topic.getTopic(), msg);
        }
    }
}
