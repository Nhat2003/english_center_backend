package com.example.English.Center.Data.dto.students;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentImportSummary {
    private int totalRows;
    private int processed;
    private int successes;
    private int skipped;
    private int failed;
    private List<StudentImportRowResult> results;
}

