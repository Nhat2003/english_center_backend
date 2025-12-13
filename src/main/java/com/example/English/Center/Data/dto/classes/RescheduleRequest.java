package com.example.English.Center.Data.dto.classes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RescheduleRequest {
    // ISO local date-time string, e.g. "2025-12-15T14:00:00"
    private String newStart;
    private String newEnd;

    // Optional: allow sending date + time parts separately
    // originalDate: yyyy-MM-dd (optional, overrides path variable if present)
    private String originalDate;
    // newDate: yyyy-MM-dd
    private String newDate;
    // newStartTime/newEndTime: HH:mm:ss
    private String newStartTime;
    private String newEndTime;

    private String reason;
    private Boolean notifyStudents = Boolean.TRUE;
}
