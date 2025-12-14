package com.example.English.Center.Data.dto.students;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentImportRowResult {
    private int rowNumber;
    private String status; // SUCCESS, FAILED, SKIPPED
    private Long userId;
    private Long studentId;
    private String message;
}

