package com.example.English.Center.Data.dto.attendance;

import com.example.English.Center.Data.entity.attendance.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceItemDto {
    @NotNull(message = "studentId is required")
    private Long studentId;

    @NotNull(message = "status is required")
    private AttendanceStatus status;

    private String note;
}
