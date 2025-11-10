package com.example.English.Center.Data.dto.courses;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class CourseResponse {
    private Long id;
    private String name;
    private Integer duration;
    private BigDecimal fee;
    private String description;
}
