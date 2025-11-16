package com.example.English.Center.Data.dto.classes;


import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleItemDTO {
    private String id;
    private String title;
    private String start;
    private String end;
    private String date; // new: specific date (ISO yyyy-MM-dd)
    private Integer classId;
    private Integer teacherId;
    private Integer roomId;

    // Additional fields for frontend convenience
    private String className;
    private String courseName;
    private String teacherName;
    private String roomName;
    private Integer studentId;
    private Integer sessionIndex;
    private Integer totalSessions;
    private FixedScheduleDTO fixedSchedule;

    // Thêm danh sách học sinh cho lịch giáo viên
    private List<StudentInfo> students;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentInfo {
        private Long id;
        private String fullName;
    }
}
