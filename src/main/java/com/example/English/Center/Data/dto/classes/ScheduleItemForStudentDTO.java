package com.example.English.Center.Data.dto.classes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleItemForStudentDTO {
    private String id;
    private String title;
    private String start;
    private String end;
    private Integer classId;
    private Integer teacherId;
    private Integer roomId;
    private String className;
    private String courseName;
    private String teacherName;
    private String roomName;
    private Integer sessionIndex;
    private Integer totalSessions;
    private FixedScheduleDTO fixedSchedule;
}

