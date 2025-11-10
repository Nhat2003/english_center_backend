package com.example.English.Center.Data.dto.chat;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatConversationDTO {
    private String conversationId;
    private Long otherUserId;
    private String otherUserName;
    private String lastMessage;
    private LocalDateTime lastAt;
    private Long unreadCount;
}

