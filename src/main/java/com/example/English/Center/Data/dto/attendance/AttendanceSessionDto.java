package com.example.English.Center.Data.dto.attendance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSessionDto {
    @NotNull(message = "classId is required")
    private Long classId;

    @NotNull(message = "sessionDate is required")
    private LocalDate sessionDate;

    @NotEmpty(message = "items must not be empty")
    @Valid
    private List<AttendanceItemDto> items;
}
