package com.example.English.Center.Data.service.submisssion;

import com.example.English.Center.Data.entity.submission.Submission;
import com.example.English.Center.Data.repository.submission.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;

    public SubmissionService(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    public List<Submission> getAll() {
        return submissionRepository.findAll();
    }

    public Optional<Submission> getById(Long id) {
        return submissionRepository.findById(id);
    }

    public List<Submission> getByAssignmentId(Long assignmentId) {
        return submissionRepository.findByAssignmentId(assignmentId);
    }

    public List<Submission> getByStudentId(Long studentId) {
        return submissionRepository.findByStudentId(studentId);
    }

    public Submission save(Submission submission) {
        return submissionRepository.save(submission);
    }

    public void delete(Long id) {
        submissionRepository.deleteById(id);
    }

    @Transactional
    public Submission saveOrUpdateSubmission(Submission submission) {
        Long studentId = submission.getStudent().getId();
        Long assignmentId = submission.getAssignment().getId();

        // Check if a submission already exists for this student and assignment
        Optional<Submission> existingSubmission = submissionRepository.findByStudentIdAndAssignmentId(studentId, assignmentId);
        existingSubmission.ifPresent(sub -> {
            // Delete the old submission
            submissionRepository.delete(sub);
        });

        // Save the new submission
        return submissionRepository.save(submission);
    }
}
