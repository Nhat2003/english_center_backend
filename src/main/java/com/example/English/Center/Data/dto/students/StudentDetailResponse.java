package com.example.English.Center.Data.dto.students;

import com.example.English.Center.Data.dto.submission.SubmissionResponse;
import com.example.English.Center.Data.entity.attendance.Attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class StudentDetailResponse {
    private StudentResponse student;
    // Use a lightweight DTO for attendance details to avoid exposing entities directly
    private List<AttendanceDetail> attendance;
    private List<SubmissionResponse> submissions;
    private Map<String, Long> attendanceSummary;

    public StudentDetailResponse() {}

    public StudentDetailResponse(StudentResponse student, List<AttendanceDetail> attendance, List<SubmissionResponse> submissions, Map<String, Long> attendanceSummary) {
        this.student = student;
        this.attendance = attendance;
        this.submissions = submissions;
        this.attendanceSummary = attendanceSummary;
    }

    public StudentResponse getStudent() {
        return student;
    }

    public List<AttendanceDetail> getAttendance() {
        return attendance;
    }

    public List<SubmissionResponse> getSubmissions() {
        return submissions;
    }

    public Map<String, Long> getAttendanceSummary() {
        return attendanceSummary;
    }

    // Lightweight DTO to represent attendance per session
    public static class AttendanceDetail {
        private Long id;
        private LocalDate sessionDate;
        private Long classId;
        private String className;
        private String status;
        private String note;

        public AttendanceDetail() {}

        public AttendanceDetail(Attendance a) {
            if (a == null) return;
            this.id = a.getId();
            this.sessionDate = a.getSessionDate();
            if (a.getClassRoom() != null) {
                this.classId = a.getClassRoom().getId();
                try {
                    this.className = a.getClassRoom().getName();
                } catch (Exception ignored) {
                    // name might be null or cause lazy loading issues; ignore to keep DTO safe
                }
            }
            this.status = a.getStatus() != null ? a.getStatus().name() : null;
            this.note = a.getNote();
        }

        public Long getId() {
            return id;
        }

        public LocalDate getSessionDate() {
            return sessionDate;
        }

        public Long getClassId() {
            return classId;
        }

        public String getClassName() {
            return className;
        }

        public String getStatus() {
            return status;
        }

        public String getNote() {
            return note;
        }
    }
}
