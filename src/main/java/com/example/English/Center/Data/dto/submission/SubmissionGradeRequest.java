package com.example.English.Center.Data.dto.submission;

import java.time.LocalDateTime;

public class SubmissionGradeRequest {
    private Double grade;
    private String feedback;
    private LocalDateTime createdAt;

    public SubmissionGradeRequest() {}

    public Double getGrade() {
        return grade;
    }

    public void setGrade(Double grade) {
        this.grade = grade;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
