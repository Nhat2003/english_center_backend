package com.example.English.Center.Data.dto.classes;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateClassRequest {
    private String name;
    private Long courseId;
    private Long teacherId;
    private Long roomId;
    private Long fixedScheduleId;
    private LocalDate startDate;
    private List<Long> studentIds;

}

