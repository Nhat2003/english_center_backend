package com.example.English.Center.Data.entity.chat;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "is_read")
    @Builder.Default
    private Boolean isRead = false;

    // conversation id (stable key between two users) e.g. "user-1_user-5" (lower id first)
    private String conversationId;

    // ensure sentAt and isRead have sensible defaults when persisted
    @PrePersist
    public void prePersist() {
        if (this.sentAt == null) this.sentAt = LocalDateTime.now();
        if (this.isRead == null) this.isRead = Boolean.FALSE;
    }
}
