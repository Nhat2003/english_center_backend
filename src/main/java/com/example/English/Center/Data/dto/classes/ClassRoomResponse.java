package com.example.English.Center.Data.dto.classes;

import lombok.Data;

import java.util.List;

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
    private List<Long> students;

    public static class FixedScheduleInfo {
        private String name;
        private String daysOfWeek;
        private String startTime;
        private String endTime;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDaysOfWeek() { return daysOfWeek; }
        public void setDaysOfWeek(String daysOfWeek) { this.daysOfWeek = daysOfWeek; }
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
    }
    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public FixedScheduleInfo getFixedSchedule() { return fixedSchedule; }
    public void setFixedSchedule(FixedScheduleInfo fixedSchedule) { this.fixedSchedule = fixedSchedule; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public int getStudentCount() { return studentCount; }
    public void setStudentCount(int studentCount) { this.studentCount = studentCount; }
    public List<Long> getStudents() { return students; }
    public void setStudents(List<Long> students) { this.students = students; }
}
