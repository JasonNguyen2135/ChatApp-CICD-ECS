package com.project.chatapp.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String text;

    private String senderId;
    private String receiverId;
    private Long timestamp;

    private String type;

    @Column(columnDefinition = "TEXT")
    private String fileUrl;

    // --- QUAN TRỌNG: Dùng Boolean (chữ B hoa) ---
    // Để nếu Android không gửi trường này, nó sẽ là null chứ không gây crash
    @JsonProperty("isRevoked")
    private Boolean isRevoked = false;

    @JsonProperty("isPinned")
    private Boolean isPinned = false;

    private String replyToId;

    @Column(columnDefinition = "TEXT")
    private String replyToText;

    private String reaction;

    public void setIsRevoked(boolean isRevoked) {
        this.isRevoked = isRevoked;
    }
}