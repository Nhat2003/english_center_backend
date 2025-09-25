package com.example.English.Center.Data.dto.classes;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClassRoomRequest {
    @NotBlank(message = "Class name is required")
    private String name;
    @NotNull(message = "Schedule id is required")
    private Long scheduleId;
    @NotNull(message = "Teacher id is required")
    private Long teacherId;
    private List<Long> studentIds;
}
