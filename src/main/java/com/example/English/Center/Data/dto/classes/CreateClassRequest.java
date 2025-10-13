package com.example.English.Center.Data.dto.classes;

import java.time.LocalDate;
import java.util.List;

public class CreateClassRequest {
    private String name;
    private Long courseId;
    private Long teacherId;
    private Long roomId;
    private Long fixedScheduleId;
    private LocalDate startDate;
    private List<Long> studentIds;

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public Long getFixedScheduleId() { return fixedScheduleId; }
    public void setFixedScheduleId(Long fixedScheduleId) { this.fixedScheduleId = fixedScheduleId; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public List<Long> getStudentIds() { return studentIds; }
    public void setStudentIds(List<Long> studentIds) { this.studentIds = studentIds; }
}

