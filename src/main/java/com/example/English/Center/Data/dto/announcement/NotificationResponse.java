package com.example.English.Center.Data.dto.announcement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private Long classId;
    private Long announcementId;
    private String announcementTitle;
    private String announcementContent;
    private boolean read;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}

