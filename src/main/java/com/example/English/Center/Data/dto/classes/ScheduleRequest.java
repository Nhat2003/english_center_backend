package com.example.English.Center.Data.dto.classes;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class ScheduleRequest {
    @NotBlank(message = "Schedule name is required")
    private String name;
    private String description;
}

