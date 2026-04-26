package com.project.chatapp.controller;

import com.project.chatapp.model.ChatMessage;
import com.project.chatapp.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageRepository messageRepository;

    // Gửi tin nhắn qua WebSocket
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        // ✅ LƯU VÀO DATABASE TRƯỚC KHI GỬI ĐI
        ChatMessage savedMsg = messageRepository.save(chatMessage);
        
        // Gửi tới người nhận
        messagingTemplate.convertAndSend("/topic/messages/" + chatMessage.getReceiverId(), savedMsg);
        // Gửi ngược lại người gửi để đồng bộ UI
        messagingTemplate.convertAndSend("/topic/messages/" + chatMessage.getSenderId(), savedMsg);
    }

    // ✅ API LẤY LỊCH SỬ CHAT (Để vào lại không bị mất tin nhắn)
    @GetMapping("/api/messages/{senderId}/{receiverId}")
    public List<ChatMessage> getChatHistory(@PathVariable String senderId, @PathVariable String receiverId) {
        return messageRepository.findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByTimestampAsc(
                senderId, receiverId, receiverId, senderId);
    }
}
