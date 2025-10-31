package com.example.English.Center.Data.repository.submission;

import com.example.English.Center.Data.entity.submission.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByAssignmentId(Long assignmentId);
    List<Submission> findByStudentId(Long studentId);
    Optional<Submission> findByStudentIdAndAssignmentId(Long studentId, Long assignmentId);
}
