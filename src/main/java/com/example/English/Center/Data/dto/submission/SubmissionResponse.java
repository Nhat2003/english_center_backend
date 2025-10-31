package com.example.English.Center.Data.dto.submission;

import com.example.English.Center.Data.entity.submission.Submission;

import java.time.LocalDateTime;

public class SubmissionResponse {
    private Long id;
    private Long assignmentId;
    private Long studentId;
    private LocalDateTime submittedAt;
    private String fileUrl;
    private Double grade;
    private String feedback;

    public SubmissionResponse(Submission s) {
        if (s == null) return;
        this.id = s.getId();
        this.assignmentId = s.getAssignment() != null ? s.getAssignment().getId() : null;
        this.studentId = s.getStudent() != null ? s.getStudent().getId() : null;
        this.submittedAt = s.getSubmittedAt();
        this.fileUrl = s.getFileUrl();
        this.grade = s.getGrade();
        this.feedback = s.getFeedback();
    }

    public Long getId() {
        return id;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public Double getGrade() {
        return grade;
    }

    public String getFeedback() {
        return feedback;
    }
}

