package com.example.English.Center.Data.dto.announcement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementResponse {
    private Long id;
    private Long classId;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private Long createdByTeacherId;
}

