package com.example.English.Center.Data.controller.submission;

import com.example.English.Center.Data.dto.submission.SubmissionGradeRequest;
import com.example.English.Center.Data.dto.submission.SubmissionResponse;
import com.example.English.Center.Data.entity.submission.Submission;
import com.example.English.Center.Data.entity.assignment.Assignment;
import com.example.English.Center.Data.entity.students.Student;
import com.example.English.Center.Data.entity.teachers.Teacher;
import com.example.English.Center.Data.entity.users.User;
import com.example.English.Center.Data.repository.assignment.AssignmentRepository;
import com.example.English.Center.Data.repository.students.StudentRepository;
import com.example.English.Center.Data.repository.teachers.TeacherRepository;
import com.example.English.Center.Data.repository.users.UserRepository;
import com.example.English.Center.Data.service.file.FileStorageService;
import com.example.English.Center.Data.service.submisssion.SubmissionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final FileStorageService fileStorageService;
    private final AssignmentRepository assignmentRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;

    public SubmissionController(SubmissionService submissionService,
                                FileStorageService fileStorageService,
                                AssignmentRepository assignmentRepository,
                                StudentRepository studentRepository,
                                UserRepository userRepository,
                                TeacherRepository teacherRepository) {
        this.submissionService = submissionService;
        this.fileStorageService = fileStorageService;
        this.assignmentRepository = assignmentRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
    }

    @GetMapping
    public List<SubmissionResponse> getAll() {
        return submissionService.getAll().stream().map(SubmissionResponse::new).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public SubmissionResponse getById(@PathVariable Long id) {
        Submission s = submissionService.getById(id).orElse(null);
        return s == null ? null : new SubmissionResponse(s);
    }

    @GetMapping("/assignment/{assignmentId}")
    public List<SubmissionResponse> getByAssignment(@PathVariable Long assignmentId) {
        return submissionService.getByAssignmentId(assignmentId).stream().map(SubmissionResponse::new).collect(Collectors.toList());
    }

    @GetMapping("/student/{studentId}")
    public List<SubmissionResponse> getByStudent(@PathVariable Long studentId) {
        return submissionService.getByStudentId(studentId).stream().map(SubmissionResponse::new).collect(Collectors.toList());
    }

    @PostMapping
    public SubmissionResponse create(@RequestBody Submission submission) {
        submission.setSubmittedAt(LocalDateTime.now());
        Submission saved = submissionService.save(submission);
        return new SubmissionResponse(saved);
    }

    @PutMapping("/{id}")
    public SubmissionResponse update(@PathVariable Long id, @RequestBody Submission submission) {
        submission.setId(id);
        Submission updated = submissionService.save(submission);
        return new SubmissionResponse(updated);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        submissionService.delete(id);
    }

    // New endpoint: student uploads a submission file (existing /upload kept for backward compatibility)
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SubmissionResponse uploadSubmissionFile(@RequestParam("file") MultipartFile file,
                                           @RequestParam("assignmentId") Long assignmentId,
                                           @RequestParam("studentId") Long studentId) throws Exception {
        String fileUrl = fileStorageService.storeSubmissionFile(file);

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found with id " + assignmentId));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id " + studentId));

        Submission submission = new Submission();
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setFileUrl(fileUrl);

        Submission saved = submissionService.save(submission);
        return new SubmissionResponse(saved);
    }

    // Preferred endpoint: POST /submissions/assignment/{assignmentId} (multipart)
    @PostMapping(value = "/assignment/{assignmentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SubmissionResponse submitForAssignment(@PathVariable("assignmentId") Long assignmentId,
                                          @RequestParam(value = "file", required = false) MultipartFile file,
                                          @RequestParam("studentId") Long studentId) throws Exception {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found with id " + assignmentId));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id " + studentId));

        String fileUrl = null;
        if (file != null && !file.isEmpty()) {
            fileUrl = fileStorageService.storeSubmissionFile(file);
        }

        Submission submission = new Submission();
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setFileUrl(fileUrl);

        Submission saved = submissionService.save(submission);
        return new SubmissionResponse(saved);
    }

    // New endpoint: teacher/admin grade a submission
    @PutMapping("/{id}/grade")
    public SubmissionResponse gradeSubmission(@PathVariable Long id, @RequestBody SubmissionGradeRequest request) {
        Submission submission = submissionService.getById(id)
                .orElseThrow(() -> new RuntimeException("Submission not found with id " + id));

        // Ensure assignment exists and get the assigned teacher id
        Assignment assignment = submission.getAssignment();
        if (assignment == null || assignment.getTeacher() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignment or assigned teacher information missing");
        }
        Long assignedTeacherId = assignment.getTeacher().getId();

        // Get authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authenticated");
        }
        String username = auth.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User not found"));

        // If user has ADMIN role, allow grading
        boolean isAdmin = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_ADMIN"));
        if (!isAdmin) {
            // Resolve teacher entity for this user
            Teacher teacher = teacherRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher profile not found for user"));
            Long requesterTeacherId = teacher.getId();
            if (!requesterTeacherId.equals(assignedTeacherId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the assigned teacher can grade this submission");
            }
        }

        Double grade = request.getGrade();
        if (grade != null) {
            if (grade < 0.0 || grade > 10.0) {
                throw new IllegalArgumentException("Grade must be between 0 and 10");
            }
            submission.setGrade(grade);
        }

        if (request.getFeedback() != null) {
            submission.setFeedback(request.getFeedback());
        }

        Submission saved = submissionService.save(submission);
        return new SubmissionResponse(saved);
    }
}