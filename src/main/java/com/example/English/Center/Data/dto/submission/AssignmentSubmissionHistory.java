package com.example.English.Center.Data.dto.submission;

import java.util.List;

public class AssignmentSubmissionHistory {
    private Long assignmentId;
    private String assignmentTitle;
    private List<SubmissionResponse> submissions;
    private Double latestGrade;

    public AssignmentSubmissionHistory() {}

    public AssignmentSubmissionHistory(Long assignmentId, String assignmentTitle, List<SubmissionResponse> submissions, Double latestGrade) {
        this.assignmentId = assignmentId;
        this.assignmentTitle = assignmentTitle;
        this.submissions = submissions;
        this.latestGrade = latestGrade;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public String getAssignmentTitle() {
        return assignmentTitle;
    }

    public List<SubmissionResponse> getSubmissions() {
        return submissions;
    }

    public Double getLatestGrade() {
        return latestGrade;
    }
}

