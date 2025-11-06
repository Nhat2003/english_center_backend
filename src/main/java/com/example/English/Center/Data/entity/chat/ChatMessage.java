package com.example.English.Center.Data.entity.chat;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // sender user id
    private Long senderId;

    // receiver user id (single-receiver chat). For group chat you'd change model.
    private Long receiverId;

    @Column(columnDefinition = "text")
    private String content;

    private LocalDateTime sentAt;

    private Boolean isRead = false;

    // conversation id (stable key between two users) e.g. "user-1_user-5" (lower id first)
    private String conversationId;
}

