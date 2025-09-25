package com.example.English.Center.Data.dto.classes;

import java.util.List;
import lombok.Data;

@Data
public class ClassRoomResponse {
    private Long id;
    private String name;
    private String schedule;
    private Long teacherId;
    private String teacherName;
    private List<Long> studentIds;
    private List<String> studentNames;
    private Long scheduleId;
    private String scheduleName;
    private String scheduleDescription;
}
