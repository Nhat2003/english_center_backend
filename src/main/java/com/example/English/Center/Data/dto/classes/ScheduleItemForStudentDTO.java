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
    private String date; // new: specific date (ISO yyyy-MM-dd)
    private Integer classId;
    private Integer teacherId;
    private String teacherName;
    private Integer roomId;
    private String className;
    private String courseName;
    private String roomName;
    private Integer sessionIndex;
    private Integer totalSessions;
    private FixedScheduleDTO fixedSchedule;
}
