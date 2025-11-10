package com.example.English.Center.Data.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatContactDTO {
    private Long id;        // userId
    private String fullName;
    private String role;    // STUDENT or TEACHER or ADMIN
}

