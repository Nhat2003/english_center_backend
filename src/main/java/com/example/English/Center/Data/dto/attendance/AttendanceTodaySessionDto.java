package com.example.English.Center.Data.dto.attendance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceTodaySessionDto {
    @NotNull(message = "classId is required")
    private Long classId;

    @NotEmpty(message = "items must not be empty")
    @Valid
    private List<AttendanceItemDto> items;
}

