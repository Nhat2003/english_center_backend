package com.example.English.Center.Data.dto.classes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassRoomResponse {
    private Long id;
    private String name;
    private String teacherName;
    private String courseName;
    private String courseDescription;
    private String roomName;
    private FixedScheduleInfo fixedSchedule;
    private String startDate;
    private String endDate;
    private int studentCount;
    private Set<Long> students;

    // Explicit getters/setters added to ensure methods exist even if Lombok isn't processed
    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return this.name; }
    public void setName(String name) { this.name = name; }

    public String getTeacherName() { return this.teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public String getCourseName() { return this.courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getCourseDescription() { return this.courseDescription; }
    public void setCourseDescription(String courseDescription) { this.courseDescription = courseDescription; }

    public String getRoomName() { return this.roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public FixedScheduleInfo getFixedSchedule() { return this.fixedSchedule; }
    public void setFixedSchedule(FixedScheduleInfo fixedSchedule) { this.fixedSchedule = fixedSchedule; }

    public String getStartDate() { return this.startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return this.endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public int getStudentCount() { return this.studentCount; }
    public void setStudentCount(int studentCount) { this.studentCount = studentCount; }

    public Set<Long> getStudents() { return this.students; }
    public void setStudents(Set<Long> students) { this.students = students; }



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
