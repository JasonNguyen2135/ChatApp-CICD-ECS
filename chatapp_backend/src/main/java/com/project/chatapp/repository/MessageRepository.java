package com.project.chatapp.repository;

import com.project.chatapp.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MessageRepository extends JpaRepository<ChatMessage, Long> {
    
    // ✅ BỔ SUNG: Hàm lấy lịch sử chat giữa 2 người, sắp xếp theo thời gian
    List<ChatMessage> findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByTimestampAsc(
            String senderId1, String receiverId1, String senderId2, String receiverId2);
}
