package com.example.English.Center.Data.dto.students;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentOverviewResponse {
    // Basic
    private Long studentId;
    private String fullName;
    private String email;

    // Classes
    private int classesCount;

    // Attendance summary
    private long attendancePresent;
    private long attendanceAbsent;
    private long attendanceLate;
    private long attendanceTotal;
    private Double attendanceRate; // present/total

    // Assignments summary
    private int assignmentsTotal;        // tổng số bài tập trong các lớp
    private int assignmentsSubmitted;    // số bài đã nộp (tính theo assignment, không phải số lần nộp)
    private int assignmentsPending;      // assignmentsTotal - assignmentsSubmitted
    private int assignmentsGraded;       // số bài đã có điểm (tính theo assignment)
    private Double averageGrade;         // trung bình điểm của bài đã chấm (tính theo lần chấm mới nhất mỗi bài)

    // Today sessions
    private int todaySessionsCount;      // số buổi học hôm nay (tổng theo các lớp)

    // Notifications
    private long unreadNotifications;
}

