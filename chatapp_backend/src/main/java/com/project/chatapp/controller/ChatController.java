package com.project.chatapp.controller;

import com.project.chatapp.model.ChatMessage;
import com.project.chatapp.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Cho phép Android gọi API
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageRepository messageRepository;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        System.out.println("SERVER NHẬN MESSAGE: " + chatMessage.getText());

        // 1. Luôn ép ID về null để INSERT tin mới (đã làm)
        chatMessage.setId(null);

        // 2. MỚI: Đảm bảo các giá trị Boolean không bị null
        if (chatMessage.getIsRevoked() == null) chatMessage.setIsRevoked(false);
        if (chatMessage.getIsPinned() == null) chatMessage.setIsPinned(false);

        // 3. Kiểm tra timestamp
        if (chatMessage.getTimestamp() == null || chatMessage.getTimestamp() == 0) {
            chatMessage.setTimestamp(System.currentTimeMillis());
        }

        ChatMessage savedMsg = messageRepository.save(chatMessage);
        System.out.println("ĐÃ LƯU VÀO DB: ID = " + savedMsg.getId());

        // 4. Gửi tin cho người nhận và người gửi
        messagingTemplate.convertAndSend("/topic/messages/" + chatMessage.getReceiverId(), savedMsg);
        messagingTemplate.convertAndSend("/topic/messages/" + chatMessage.getSenderId(), savedMsg);
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

        // 1. Tìm tin nhắn cũ và cập nhật cảm xúc
        ChatMessage msg = messageRepository.findById(messageId).orElse(null);
        if (msg != null) {
            msg.setReaction(reaction);
            messageRepository.save(msg);

            // 2. Gửi lại cho cả người gửi và người nhận để cập nhật UI Realtime
            messagingTemplate.convertAndSend("/topic/messages/" + msg.getReceiverId(), msg);
            messagingTemplate.convertAndSend("/topic/messages/" + msg.getSenderId(), msg);
        }
    }
    @MessageMapping("/chat.revokeMessage")
    public void revokeMessage(@Payload Map<String, Object> payload) {
        Long messageId = Long.valueOf(payload.get("id").toString());
        ChatMessage msg = messageRepository.findById(messageId).orElse(null);
        if (msg != null) {
            msg.setIsRevoked(true); // Đánh dấu thu hồi
            msg.setText("Tin nhắn đã bị thu hồi");
            messageRepository.save(msg);

            // Gửi thông báo cập nhật cho cả 2 bên qua WebSocket
            messagingTemplate.convertAndSend("/topic/messages/" + msg.getReceiverId(), msg);
            messagingTemplate.convertAndSend("/topic/messages/" + msg.getSenderId(), msg);
        }
    }
}