package com.example.English.Center.Data.dto.courses;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CourseRequest {
    @NotBlank(message = "Course name is required")
    private String name;
    @NotNull(message = "Duration is required")
    private Integer duration;
    @NotNull(message = "Fee is required")
    private BigDecimal fee;
}

