package com.project.chatapp.controller;

import com.project.chatapp.model.ChatMessage;
import com.project.chatapp.model.User;
import com.project.chatapp.repository.MessageRepository;
import com.project.chatapp.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    
    // ✅ Thêm Redis để phát tin nhắn
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic topic;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        // 1. Lưu vào Database
        ChatMessage savedMsg = messageRepository.save(chatMessage);
        
        // 2. ✅ QUAN TRỌNG: Quăng tin nhắn lên Redis thay vì gửi trực tiếp
        // Tất cả các Task (bao gồm cả Task này) sẽ nghe thấy và đẩy xuống máy User
        try {
            String jsonMessage = objectMapper.writeValueAsString(savedMsg);
            redisTemplate.convertAndSend(topic.getTopic(), jsonMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/api/messages/{senderId}/{receiverId}")
    public List<ChatMessage> getChatHistory(@PathVariable String senderId, @PathVariable String receiverId) {
        return messageRepository.findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByTimestampAsc(
                senderId, receiverId, receiverId, senderId);
    }

    @GetMapping("/api/messages/conversations/{userId}")
    public List<User> getConversations(@PathVariable String userId) {
        List<ChatMessage> allMessages = messageRepository.findAll();
        Set<String> partnerIds = allMessages.stream()
                .filter(m -> m.getSenderId().equals(userId) || m.getReceiverId().equals(userId))
                .flatMap(m -> Stream.of(m.getSenderId(), m.getReceiverId()))
                .filter(id -> !id.equals(userId))
                .collect(Collectors.toSet());

        return userRepository.findAllById(partnerIds);
    }
}
