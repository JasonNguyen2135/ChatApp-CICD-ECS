package com.project.chatapp.repository;

import com.project.chatapp.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<ChatMessage, Long> {

    // Tìm tin nhắn giữa 2 người (bất kể ai gửi trước) và sắp xếp theo thời gian
    @Query("SELECT m FROM ChatMessage m WHERE " +
            "(m.senderId = :user1 AND m.receiverId = :user2) OR " +
            "(m.senderId = :user2 AND m.receiverId = :user1) " +
            "ORDER BY m.timestamp ASC")
    List<ChatMessage> findChatHistory(String user1, String user2);
}