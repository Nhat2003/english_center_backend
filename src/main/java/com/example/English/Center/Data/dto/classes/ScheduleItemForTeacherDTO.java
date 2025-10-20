package com.example.English.Center.Data.dto.classes;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleItemForTeacherDTO {
    private String id;
    private String title;
    private String start;
    private String end;
    private Integer classId;
    private Integer roomId;
    private String className;
    private String courseName;
    private String roomName;
    private Integer sessionIndex;
    private Integer totalSessions;
    private FixedScheduleDTO fixedSchedule;
    // Danh sách sinh viên
    private List<ScheduleItemDTO.StudentInfo> students;
}

