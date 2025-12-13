package com.example.English.Center.Data.dto.classes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RescheduleResponse {
    private Long overrideId;
    private Long classId;
    private String originalDate;
    private String newStart;
    private String newEnd;
    private String status;
}

