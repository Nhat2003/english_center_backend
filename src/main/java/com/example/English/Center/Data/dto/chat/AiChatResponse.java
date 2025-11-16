package com.example.English.Center.Data.dto.chat;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiChatResponse {
    private String reply;
    private ChatMessageDTO savedUserMessage;
    private ChatMessageDTO savedAiMessage;

    // Optional instruction for frontend: e.g. "NAVIGATE" with payload "/schedule" or structured JSON
    private String action; // e.g. NAVIGATE, SHOW_SCHEDULE, SHOW_GRADES, SHOW_ATTENDANCE
    private String actionPayload; // e.g. path or additional JSON payload
}
