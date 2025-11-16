package com.example.English.Center.Data.dto.chat;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiChatRequest {
    // user message to send to AI
    private String message;
    // optional: keep messages tied to a conversation with another user (not required)
    private Long otherUserId;
}

