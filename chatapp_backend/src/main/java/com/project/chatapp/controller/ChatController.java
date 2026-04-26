package com.project.chatapp.controller;

import com.project.chatapp.model.ChatMessage;
import com.project.chatapp.model.User;
import com.project.chatapp.repository.MessageRepository;
import com.project.chatapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        ChatMessage savedMsg = messageRepository.save(chatMessage);
        messagingTemplate.convertAndSend("/topic/messages/" + chatMessage.getReceiverId(), savedMsg);
        messagingTemplate.convertAndSend("/topic/messages/" + chatMessage.getSenderId(), savedMsg);
    }

    @GetMapping("/api/messages/{senderId}/{receiverId}")
    public List<ChatMessage> getChatHistory(@PathVariable String senderId, @PathVariable String receiverId) {
        return messageRepository.findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByTimestampAsc(
                senderId, receiverId, receiverId, senderId);
    }

    // ✅ BỔ SUNG: Lấy danh sách những người đã từng nhắn tin
    @GetMapping("/api/messages/conversations/{userId}")
    public List<User> getConversations(@PathVariable String userId) {
        List<ChatMessage> allMessages = messageRepository.findAll();
        
        // Lấy danh sách ID của những người đã nhắn tin với userId
        Set<String> partnerIds = allMessages.stream()
                .filter(m -> m.getSenderId().equals(userId) || m.getReceiverId().equals(userId))
                .flatMap(m -> Stream.of(m.getSenderId(), m.getReceiverId()))
                .filter(id -> !id.equals(userId))
                .collect(Collectors.toSet());

        return userRepository.findAllById(partnerIds);
    }
}
