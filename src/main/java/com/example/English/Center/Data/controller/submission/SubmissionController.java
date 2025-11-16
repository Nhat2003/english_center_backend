package com.example.English.Center.Data.controller.submission;

import com.example.English.Center.Data.dto.submission.SubmissionGradeRequest;
import com.example.English.Center.Data.dto.submission.SubmissionResponse;
import com.example.English.Center.Data.dto.submission.AssignmentSubmissionHistory;
import com.example.English.Center.Data.entity.submission.Submission;
import com.example.English.Center.Data.entity.assignment.Assignment;
import com.example.English.Center.Data.entity.students.Student;
import com.example.English.Center.Data.entity.teachers.Teacher;
import com.example.English.Center.Data.entity.users.User;
import com.example.English.Center.Data.repository.assignment.AssignmentRepository;
import com.example.English.Center.Data.repository.students.StudentRepository;
import com.example.English.Center.Data.repository.teachers.TeacherRepository;
import com.example.English.Center.Data.repository.users.UserRepository;
import com.example.English.Center.Data.repository.classes.ClassEntityRepository;
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
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import java.nio.file.Path;
import java.nio.file.Paths;


@RestController
@RequestMapping("/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final FileStorageService fileStorageService;
    private final AssignmentRepository assignmentRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final ClassEntityRepository classRepository;

    public SubmissionController(SubmissionService submissionService,
                                FileStorageService fileStorageService,
                                AssignmentRepository assignmentRepository,
                                StudentRepository studentRepository,
                                UserRepository userRepository,
                                TeacherRepository teacherRepository,
                                ClassEntityRepository classRepository) {
        this.submissionService = submissionService;
        this.fileStorageService = fileStorageService;
        this.assignmentRepository = assignmentRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
        this.classRepository = classRepository;
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
    public SubmissionResponse uploadSubmissionFile(@RequestParam(value = "file", required = false) MultipartFile file,
                                           @RequestParam("assignmentId") Long assignmentId,
                                           @RequestParam("studentId") Long studentId,
                                           @RequestParam(value = "content", required = false) String content) throws Exception {
        // Authorization: only the student (owner) or admin can upload on behalf of student
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        boolean isAdmin = auth.getAuthorities().stream().map(a -> a.getAuthority()).anyMatch(a -> a.equals("ROLE_ADMIN"));
        if (!isAdmin) {
            User user = userRepository.findByUsername(auth.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User not found"));
            var studentOpt = studentRepository.findByUserId(user.getId());
            if (studentOpt.isEmpty() || !studentOpt.get().getId().equals(studentId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: you can only upload submissions for your own student account");
            }
        }
        String fileUrl = null;
        if ((file == null || file.isEmpty()) && (content == null || content.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Either file or content must be provided");
        }
        String originalFilename = null;
        if (file != null && !file.isEmpty()) {
            originalFilename = org.springframework.util.StringUtils.cleanPath(file.getOriginalFilename());
            fileUrl = fileStorageService.storeSubmissionFile(file);
        }

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found with id " + assignmentId));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found with id " + studentId));

        // ensure student belongs to assignment's class
        if (assignment.getClassRoom() == null || assignment.getClassRoom().getStudents() == null || assignment.getClassRoom().getStudents().stream().noneMatch(s -> s.getId().equals(student.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student is not a member of the assignment's class");
        }

        Submission submission = new Submission();
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setFileUrl(fileUrl);
        submission.setOriginalFilename(originalFilename);
        submission.setContent(content);

        Submission saved = submissionService.save(submission);
        return new SubmissionResponse(saved);
    }

    // Preferred endpoint: POST /submissions/assignment/{assignmentId} (multipart)
    @PostMapping(value = "/assignment/{assignmentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SubmissionResponse submitForAssignment(@PathVariable("assignmentId") Long assignmentId,
                                          @RequestParam(value = "file", required = false) MultipartFile file,
                                          @RequestParam("studentId") Long studentId,
                                          @RequestParam(value = "content", required = false) String content) throws Exception {
        // Authorization: only the authenticated student (or admin) can submit for themselves
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        boolean isAdmin = auth.getAuthorities().stream().map(a -> a.getAuthority()).anyMatch(a -> a.equals("ROLE_ADMIN"));
        if (!isAdmin) {
            User user = userRepository.findByUsername(auth.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User not found"));
            var studentOpt = studentRepository.findByUserId(user.getId());
            if (studentOpt.isEmpty() || !studentOpt.get().getId().equals(studentId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: students may only submit their own work");
            }
        }

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found with id " + assignmentId));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found with id " + studentId));

        String fileUrl = null;
        if ((file == null || file.isEmpty()) && (content == null || content.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Either file or content must be provided");
        }
        String originalFilename = null;
        if (file != null && !file.isEmpty()) {
            originalFilename = org.springframework.util.StringUtils.cleanPath(file.getOriginalFilename());
            fileUrl = fileStorageService.storeSubmissionFile(file);
        }

        // ensure student belongs to assignment's class
        if (assignment.getClassRoom() == null || assignment.getClassRoom().getStudents() == null || assignment.getClassRoom().getStudents().stream().noneMatch(s -> s.getId().equals(student.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student is not a member of the assignment's class");
        }

        Submission submission = new Submission();
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setFileUrl(fileUrl);
        submission.setOriginalFilename(originalFilename);
        submission.setContent(content);

        Submission saved = submissionService.save(submission);
        return new SubmissionResponse(saved);
    }

    // New convenient endpoint: authenticated student submits for assignment (no studentId needed)
    @PostMapping(value = "/assignment/{assignmentId}/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SubmissionResponse submitForAssignmentMe(@PathVariable("assignmentId") Long assignmentId,
                                                   @RequestParam(value = "file", required = false) MultipartFile file,
                                                   @RequestParam(value = "content", required = false) String content) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");

        User user = userRepository.findByUsername(auth.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User not found"));
        var studentOpt = studentRepository.findByUserId(user.getId());
        if (studentOpt.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student profile not found for user");
        Student student = studentOpt.get();

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found with id " + assignmentId));

        // ensure student belongs to assignment's class
        if (assignment.getClassRoom() == null || assignment.getClassRoom().getStudents() == null || assignment.getClassRoom().getStudents().stream().noneMatch(s -> s.getId().equals(student.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student is not a member of the assignment's class");
        }

        String fileUrl = null;
        if ((file == null || file.isEmpty()) && (content == null || content.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Either file or content must be provided");
        }
        String originalFilename = null;
        if (file != null && !file.isEmpty()) {
            originalFilename = org.springframework.util.StringUtils.cleanPath(file.getOriginalFilename());
            fileUrl = fileStorageService.storeSubmissionFile(file);
        }

        Submission submission = new Submission();
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setFileUrl(fileUrl);
        submission.setOriginalFilename(originalFilename);
        submission.setContent(content);

        Submission saved = submissionService.save(submission);
        return new SubmissionResponse(saved);
    }

    // New endpoint: teacher/admin grade a submission
    @PutMapping("/{id}/grade")
    public SubmissionResponse gradeSubmission(@PathVariable Long id, @RequestBody SubmissionGradeRequest request) {
        Submission submission = submissionService.getById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found with id " + id));

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

    @Value("${upload.submission-dir}")
    private String submissionDir;

    // Download a submission file (teacher for the assignment, admin, or the student owner)
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadSubmissionFile(@PathVariable Long id) {
        Submission submission = submissionService.getById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found with id " + id));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");

        boolean isAdmin = auth.getAuthorities().stream().map(a -> a.getAuthority()).anyMatch(a -> a.equals("ROLE_ADMIN"));
        boolean allowed = false;
        if (isAdmin) allowed = true;

        // teacher check: must be the teacher assigned to the assignment
        if (!allowed) {
            boolean isTeacher = auth.getAuthorities().stream().map(a -> a.getAuthority()).anyMatch(a -> a.equals("ROLE_TEACHER"));
            if (isTeacher) {
                User user = userRepository.findByUsername(auth.getName()).orElse(null);
                if (user != null) {
                    var teacherOpt = teacherRepository.findByUserId(user.getId());
                    if (teacherOpt.isPresent()) {
                        var teacher = teacherOpt.get();
                        if (submission.getAssignment() != null && submission.getAssignment().getTeacher() != null && teacher.getId().equals(submission.getAssignment().getTeacher().getId())) {
                            allowed = true;
                        }
                    }
                }
            }
        }

        // student owner check
        if (!allowed) {
            boolean isStudent = auth.getAuthorities().stream().map(a -> a.getAuthority()).anyMatch(a -> a.equals("ROLE_STUDENT"));
            if (isStudent) {
                User user = userRepository.findByUsername(auth.getName()).orElse(null);
                if (user != null) {
                    var studentOpt = studentRepository.findByUserId(user.getId());
                    if (studentOpt.isPresent() && submission.getStudent() != null && submission.getStudent().getId().equals(studentOpt.get().getId())) {
                        allowed = true;
                    }
                }
            }
        }

        if (!allowed) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to download this submission");

        String fileUrl = submission.getFileUrl();
        String content = submission.getContent();

        // If there is no uploaded file but there is textual content, stream it as a .txt download
        if (fileUrl == null || fileUrl.isBlank()) {
            if (content == null || content.isBlank()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission has no file or text content");
            }

            String downloadFileName = submission.getOriginalFilename() != null && !submission.getOriginalFilename().isBlank()
                    ? submission.getOriginalFilename()
                    : "submission-" + submission.getId() + ".txt";

            byte[] bytes = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(bytes);

            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.TEXT_PLAIN)
                    .header(HttpHeaders.CONTENT_DISPOSITION, buildContentDisposition(downloadFileName))
                    .contentLength(bytes.length)
                    .body(resource);
        }

        String filename = StringUtils.getFilename(fileUrl);
        if (filename == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        // prefer original filename in Content-Disposition if present
        String downloadFileName = submission.getOriginalFilename() != null && !submission.getOriginalFilename().isBlank()
                ? submission.getOriginalFilename() : filename;

        try {
            Path filePath = Paths.get(submissionDir).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found on server");

            MediaType mediaType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, buildContentDisposition(downloadFileName))
                    .body(resource);
        } catch (java.net.MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read file", e);
        }
    }

    private String buildContentDisposition(String filename) {
        // Basic ASCII fallback and RFC5987 filename* for UTF-8
        String clean = filename.replaceAll("\\\"", "'");
        String encoded = java.net.URLEncoder.encode(clean, java.nio.charset.StandardCharsets.UTF_8);
        return "attachment; filename=\"" + clean + "\"; filename*=UTF-8''" + encoded;
    }

    @GetMapping("/me")
    public List<SubmissionResponse> getMySubmissions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        User user = userRepository.findByUsername(auth.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User not found"));
        var studentOpt = studentRepository.findByUserId(user.getId());
        if (studentOpt.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student profile not found for user");
        Long studentId = studentOpt.get().getId();
        return submissionService.getByStudentId(studentId).stream().map(SubmissionResponse::new).collect(Collectors.toList());
    }

    @GetMapping("/me/history")
    public List<com.example.English.Center.Data.dto.submission.AssignmentSubmissionHistory> getMySubmissionHistory(
            @RequestParam(value = "gradedOnly", required = false, defaultValue = "false") boolean gradedOnly) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        User user = userRepository.findByUsername(auth.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User not found"));
        var studentOpt = studentRepository.findByUserId(user.getId());
        if (studentOpt.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student profile not found for user");
        Long studentId = studentOpt.get().getId();

        var submissions = submissionService.getByStudentId(studentId);
        var grouped = submissions.stream().collect(java.util.stream.Collectors.groupingBy(s -> s.getAssignment() != null ? s.getAssignment().getId() : 0L));

        java.util.List<com.example.English.Center.Data.dto.submission.AssignmentSubmissionHistory> result = new java.util.ArrayList<>();
        for (var entry : grouped.entrySet()) {
            Long assignmentId = entry.getKey();
            java.util.List<Submission> list = entry.getValue();
            // sort by submittedAt DESC (nulls last)
            list.sort((a, b) -> {
                java.time.LocalDateTime at = a.getSubmittedAt();
                java.time.LocalDateTime bt = b.getSubmittedAt();
                if (at == null && bt == null) return 0;
                if (at == null) return 1;
                if (bt == null) return -1;
                return bt.compareTo(at);
            });

            // optionally keep only graded submissions
            java.util.List<Submission> filtered = gradedOnly ? list.stream().filter(s -> s.getGrade() != null).collect(java.util.stream.Collectors.toList()) : list;
            if (gradedOnly && filtered.isEmpty()) continue; // skip assignments with no graded submissions

            java.util.List<SubmissionResponse> responses = filtered.stream().map(SubmissionResponse::new).collect(java.util.stream.Collectors.toList());

            // latest non-null grade by recency
            Double latestGrade = filtered.stream()
                    .map(Submission::getGrade)
                    .filter(g -> g != null)
                    .findFirst()
                    .orElse(null);

            String title = list.size() > 0 && list.get(0).getAssignment() != null ? list.get(0).getAssignment().getTitle() : null;
            result.add(new com.example.English.Center.Data.dto.submission.AssignmentSubmissionHistory(assignmentId, title, responses, latestGrade));
        }

        return result;
    }

    @GetMapping("/student/{studentId}/history")
    public List<AssignmentSubmissionHistory> getSubmissionHistoryForStudent(@PathVariable Long studentId,
                                                                            @RequestParam(value = "gradedOnly", required = false, defaultValue = "false") boolean gradedOnly) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        User user = userRepository.findByUsername(auth.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User not found"));

        boolean isAdmin = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_ADMIN"));
        boolean isTeacher = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_TEACHER"));
        boolean isStudent = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_STUDENT"));

        // Ensure student exists
        Student student = studentRepository.findById(studentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found with id " + studentId));

        if (isAdmin) {
            // allowed
        } else if (isTeacher) {
            Teacher teacher = teacherRepository.findByUserId(user.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher profile not found for user"));
            boolean teachesStudent = classRepository.findByTeacher_Id(teacher.getId())
                    .stream()
                    .flatMap(c -> c.getStudents().stream())
                    .anyMatch(s -> s.getId().equals(studentId));
            if (!teachesStudent) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher can only view history for students in their classes");
            }
        } else if (isStudent) {
            var studentSelf = studentRepository.findByUserId(user.getId());
            if (studentSelf.isEmpty() || !studentSelf.get().getId().equals(studentId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Students can only view their own submission history");
            }
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
        }

        var submissions = submissionService.getByStudentId(studentId);
        var grouped = submissions.stream().collect(java.util.stream.Collectors.groupingBy(s -> s.getAssignment() != null ? s.getAssignment().getId() : 0L));

        java.util.List<AssignmentSubmissionHistory> result = new java.util.ArrayList<>();
        for (var entry : grouped.entrySet()) {
            Long assignmentId = entry.getKey();
            java.util.List<Submission> list = entry.getValue();
            // sort by submittedAt DESC (nulls last)
            list.sort((a, b) -> {
                java.time.LocalDateTime at = a.getSubmittedAt();
                java.time.LocalDateTime bt = b.getSubmittedAt();
                if (at == null && bt == null) return 0;
                if (at == null) return 1;
                if (bt == null) return -1;
                return bt.compareTo(at);
            });

            // optionally keep only graded submissions
            java.util.List<Submission> filtered = gradedOnly ? list.stream().filter(s -> s.getGrade() != null).collect(java.util.stream.Collectors.toList()) : list;
            if (gradedOnly && filtered.isEmpty()) continue; // skip assignments with no graded submissions

            java.util.List<SubmissionResponse> responses = filtered.stream().map(SubmissionResponse::new).collect(java.util.stream.Collectors.toList());

            // latest non-null grade by recency
            Double latestGrade = filtered.stream()
                    .map(Submission::getGrade)
                    .filter(g -> g != null)
                    .findFirst()
                    .orElse(null);
            String title = list.size() > 0 && list.get(0).getAssignment() != null ? list.get(0).getAssignment().getTitle() : null;
            result.add(new AssignmentSubmissionHistory(assignmentId, title, responses, latestGrade));
        }

        return result;
    }
}
