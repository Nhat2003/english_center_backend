package com.example.English.Center.Data.dto.classes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassRoomResponse {
    private Long id;
    private String name;
    private String courseName;
    private String teacherName;
    private String roomName;
    private FixedScheduleInfo fixedSchedule;
    private String startDate;
    private String endDate;
    private int studentCount;
    private Set<Long> students;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FixedScheduleInfo {
        private String name;
        private String daysOfWeek;
        private String startTime;
        private String endTime;
    }
}
