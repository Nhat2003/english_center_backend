package com.example.English.Center.Data.controller.students;

import com.example.English.Center.Data.dto.ChangePasswordRequest;
import com.example.English.Center.Data.dto.SuccessResponse;
import com.example.English.Center.Data.dto.profile.UpdateProfileRequest;
import com.example.English.Center.Data.dto.students.StudentDetailResponse;
import com.example.English.Center.Data.dto.students.StudentOverviewResponse;
import com.example.English.Center.Data.dto.students.StudentRequest;
import com.example.English.Center.Data.dto.students.StudentResponse;
import com.example.English.Center.Data.dto.submission.SubmissionResponse;
import com.example.English.Center.Data.entity.attendance.Attendance;
import com.example.English.Center.Data.entity.students.Student;
import com.example.English.Center.Data.entity.teachers.Teacher;
import com.example.English.Center.Data.entity.users.User;
import com.example.English.Center.Data.repository.announcement.NotificationRepository;
import com.example.English.Center.Data.repository.assignment.AssignmentRepository;
import com.example.English.Center.Data.repository.classes.ClassEntityRepository;
import com.example.English.Center.Data.repository.students.StudentRepository;
import com.example.English.Center.Data.repository.teachers.TeacherRepository;
import com.example.English.Center.Data.repository.users.UserRepository;
import com.example.English.Center.Data.service.attendance.AttendanceService;
import com.example.English.Center.Data.service.students.StudentService;
import com.example.English.Center.Data.service.submisssion.SubmissionService;
import com.example.English.Center.Data.service.users.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/students")
public class StudentController {
    private final StudentService studentService;
    private final AttendanceService attendanceService;
    private final SubmissionService submissionService;
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final ClassEntityRepository classRepository;
    private final NotificationRepository notificationRepository;
    private final AssignmentRepository assignmentRepository;
    private final UserService userService;

    public StudentController(StudentService studentService,
                             AttendanceService attendanceService,
                             SubmissionService submissionService,
                             UserRepository userRepository,
                             TeacherRepository teacherRepository,
                             StudentRepository studentRepository,
                             ClassEntityRepository classRepository,
                             NotificationRepository notificationRepository,
                             AssignmentRepository assignmentRepository,
                             UserService userService) {
        this.studentService = studentService;
        this.attendanceService = attendanceService;
        this.submissionService = submissionService;
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.classRepository = classRepository;
        this.notificationRepository = notificationRepository;
        this.assignmentRepository = assignmentRepository;
        this.userService = userService;
    }

    @GetMapping
    public List<StudentResponse> getAllStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return studentService.getAllStudents(page, size);
    }

    @GetMapping("/{id:\\d+}")
    public StudentResponse getStudentById(@PathVariable Long id) {
        return studentService.getStudentById(id);
    }

    @GetMapping("/{id}/detail")
    public StudentDetailResponse getStudentDetail(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Not authenticated");
        }

        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "User not found"));

        boolean isAdmin = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_ADMIN"));
        boolean isTeacher = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_TEACHER"));
        boolean isStudent = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_STUDENT"));

        if (isAdmin) {
            // allowed
        } else if (isTeacher) {
            // teacher may view only students in their classes
            Teacher teacher = teacherRepository.findByUserId(user.getId()).orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Teacher profile not found for user"));
            boolean teachesStudent = classRepository.findByTeacher_Id(teacher.getId())
                    .stream()
                    .flatMap(c -> c.getStudents().stream())
                    .anyMatch(s -> s.getId().equals(id));
            if (!teachesStudent) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Teacher can only view students in their classes");
            }
        } else if (isStudent) {
            Student student = studentRepository.findByUserId(user.getId()).orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Student profile not found for user"));
            if (!student.getId().equals(id)) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Students can only view their own details");
            }
        } else {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Insufficient permissions");
        }

        // assemble detail
        StudentResponse student = studentService.getStudentById(id);
        List<Attendance> attendanceEntities = attendanceService.getByStudent(id);
        // map entities to lightweight DTOs defined inside StudentDetailResponse
        List<StudentDetailResponse.AttendanceDetail> attendance = attendanceEntities.stream()
                .map(StudentDetailResponse.AttendanceDetail::new)
                .collect(Collectors.toList());

        List<SubmissionResponse> submissions = submissionService.getByStudentId(id).stream().map(SubmissionResponse::new).collect(Collectors.toList());

        // compute summary from entities (enum), not from DTO (which exposes status as String)
        long present = attendanceEntities.stream().filter(a -> a.getStatus() != null && "PRESENT".equals(a.getStatus().name())).count();
        long absent = attendanceEntities.stream().filter(a -> a.getStatus() != null && "ABSENT".equals(a.getStatus().name())).count();
        long late = attendanceEntities.stream().filter(a -> a.getStatus() != null && "LATE".equals(a.getStatus().name())).count();

        Map<String, Long> summary = Map.of(
                "present", present,
                "absent", absent,
                "late", late
        );

        return new StudentDetailResponse(student, attendance, submissions, summary);
    }

    @PostMapping
    public ResponseEntity<SuccessResponse> createStudent(@RequestBody @Valid StudentRequest request) {
        studentService.createStudent(request);
        return ResponseEntity.ok(new SuccessResponse("Student created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SuccessResponse> updateStudent(@PathVariable Long id, @RequestBody @Valid StudentRequest studentRequest) {
        studentService.updateStudent(id, studentRequest);
        return ResponseEntity.ok(new SuccessResponse("Student updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<SuccessResponse> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.ok(new SuccessResponse("Student deleted successfully"));
    }

    @GetMapping("/me/overview")
    public StudentOverviewResponse getMyOverview() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Not authenticated");
        }
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "User not found"));
        Student me = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Student profile not found for user"));

        // Classes
        var classes = classRepository.findByStudents_Id(me.getId());
        int classesCount = classes != null ? classes.size() : 0;
        Set<Long> classIds = new HashSet<>();
        if (classes != null) classes.forEach(c -> { if (c.getId() != null) classIds.add(c.getId()); });

        // Attendance
        var attendance = attendanceService.getByStudent(me.getId());
        long present = attendance.stream().filter(a -> a.getStatus() != null && "PRESENT".equals(a.getStatus().name())).count();
        long absent = attendance.stream().filter(a -> a.getStatus() != null && "ABSENT".equals(a.getStatus().name())).count();
        long late = attendance.stream().filter(a -> a.getStatus() != null && "LATE".equals(a.getStatus().name())).count();
        long total = attendance.size();
        Double attendanceRate = total > 0 ? ((double) present / (double) total) : null;

        // Assignments for these classes
        var assignments = classIds.isEmpty() ? java.util.List.<com.example.English.Center.Data.entity.assignment.Assignment>of()
                : assignmentRepository.findByClassRoom_IdIn(classIds);
        int assignmentsTotal = assignments.size();

        // Submissions of this student (to compute submitted/graded per assignment)
        var submissions = submissionService.getByStudentId(me.getId());
        // group by assignmentId
        var byAssignment = submissions.stream()
                .filter(s -> s.getAssignment() != null && s.getAssignment().getId() != null)
                .collect(java.util.stream.Collectors.groupingBy(s -> s.getAssignment().getId()));
        int assignmentsSubmitted = byAssignment.keySet().size();
        int assignmentsPending = Math.max(0, assignmentsTotal - assignmentsSubmitted);
        // latest grade per assignment
        int assignmentsGraded = 0;
        double gradeSum = 0.0;
        int gradeCount = 0;
        for (var entry : byAssignment.entrySet()) {
            var list = entry.getValue();
            list.sort((a, b) -> {
                var at = a.getSubmittedAt();
                var bt = b.getSubmittedAt();
                if (at == null && bt == null) return 0;
                if (at == null) return 1;
                if (bt == null) return -1;
                return bt.compareTo(at);
            });
            var latest = list.get(0);
            if (latest.getGrade() != null) {
                assignmentsGraded++;
                gradeSum += latest.getGrade();
                gradeCount++;
            }
        }
        Double averageGrade = gradeCount > 0 ? (gradeSum / gradeCount) : null;

        // Today sessions count
        LocalDate today = LocalDate.now();
        int todaySessions = 0;
        for (var cls : classes) {
            if (cls.getFixedSchedule() == null) continue;
            try {
                // daysOfWeek like "2,4,6" using 1..7 (Mon..Sun)
                var dowSet = new java.util.HashSet<Integer>();
                String dstr = cls.getFixedSchedule().getDaysOfWeek();
                if (dstr != null && !dstr.isBlank()) {
                    for (String p : dstr.split(",")) {
                        try { dowSet.add(Integer.parseInt(p.trim())); } catch (NumberFormatException ignored) {}
                    }
                }
                int todayDow = today.getDayOfWeek().getValue();
                boolean inRange = !today.isBefore(cls.getStartDate()) && !today.isAfter(cls.getEndDate());
                if (inRange && dowSet.contains(todayDow)) todaySessions++;
            } catch (Exception ignored) {}
        }

        // Unread notifications
        long unread = notificationRepository.countByStudent_IdAndReadFlagFalse(me.getId());

        return StudentOverviewResponse.builder()
                .studentId(me.getId())
                .fullName(me.getFullName())
                .email(me.getEmail())
                .classesCount(classesCount)
                .attendancePresent(present)
                .attendanceAbsent(absent)
                .attendanceLate(late)
                .attendanceTotal(total)
                .attendanceRate(attendanceRate)
                .assignmentsTotal(assignmentsTotal)
                .assignmentsSubmitted(assignmentsSubmitted)
                .assignmentsPending(assignmentsPending)
                .assignmentsGraded(assignmentsGraded)
                .averageGrade(averageGrade)
                .todaySessionsCount(todaySessions)
                .unreadNotifications(unread)
                .build();
    }

    @GetMapping("/me/profile")
    public ResponseEntity<StudentResponse> getMyProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Not authenticated");
        }
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "User not found"));
        Student me = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Student profile not found"));

        return ResponseEntity.ok(studentService.getStudentById(me.getId()));
    }

    @PutMapping("/me/profile")
    public ResponseEntity<StudentResponse> updateMyProfile(@RequestBody @Valid UpdateProfileRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Not authenticated");
        }
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "User not found"));
        Student me = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Student profile not found"));

        StudentResponse updated = studentService.updateMyProfile(me.getId(), request);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/me/change-password")
    public ResponseEntity<SuccessResponse> changeMyPassword(@RequestBody @Valid ChangePasswordRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Not authenticated");
        }
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "User not found"));
        Student me = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Student profile not found"));

        // UserService will handle validation and password change
        userService.changePassword(user.getId(), request.getCurrentPassword(), request.getNewPassword(), request.getConfirmPassword());

        return ResponseEntity.ok(new SuccessResponse("Đổi mật khẩu thành công!"));
    }

    @GetMapping("/admin/list")
    public ResponseEntity<?> adminListStudents(
            @RequestParam(required = false) String q,
            @RequestParam(value = "classId", required = false) String classIdStr,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // Authentication/authorization check: only admins allowed - security layer should enforce roles,
        // but do a basic check here: if not admin, return 403
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body(Map.of("error","Not authenticated"));
        }
        boolean isAdmin = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a->a.equals("ROLE_ADMIN"));
        if (!isAdmin) return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(Map.of("error","Forbidden"));

        // If no search/filter, return normal paged list
        if ((q == null || q.isBlank()) && (classIdStr == null || classIdStr.isBlank())) {
            var pageRes = studentService.getAllStudentsPage(page, size);
            Map<String,Object> resp = Map.of(
                    "page", page,
                    "size", size,
                    "totalElements", pageRes.getTotalElements(),
                    "totalPages", pageRes.getTotalPages(),
                    "data", pageRes.getContent()
            );
            return ResponseEntity.ok(resp);
        }

        // Otherwise do search (q may be null) and optional class filter, then paginate in-memory
        List<com.example.English.Center.Data.dto.students.StudentResponse> all = studentService.searchStudentsRaw(q);
        // parse classId if provided
        Long classId = null;
        if (classIdStr != null && !classIdStr.isBlank()) {
            try { classId = Long.parseLong(classIdStr); } catch (NumberFormatException nfe) { return ResponseEntity.badRequest().body(Map.of("error","classId must be a number")); }
        }
        // prepare a single final set to be used by lambdas; avoid reassigning the variable later
        java.util.Set<Long> allowed = new java.util.HashSet<>();
        boolean restrictByAllowed = false;
        if (classId != null) {
            var opt = classRepository.findById(classId);
            if (opt.isEmpty()) return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(Map.of("error","Class not found"));
            var classRoom = opt.get();
            if (classRoom.getStudents() != null) classRoom.getStudents().forEach(s -> allowed.add(s.getId()));
            restrictByAllowed = true;
        }
        long total;
        List<com.example.English.Center.Data.dto.students.StudentResponse> filteredAll;
        if (!restrictByAllowed) {
            filteredAll = all;
        } else {
            filteredAll = all.stream().filter(s -> s.getId() != null && allowed.contains(s.getId())).toList();
        }
        total = filteredAll.size();
        int from = Math.min(page * size, filteredAll.size());
        int to = Math.min(from + size, filteredAll.size());
        List<com.example.English.Center.Data.dto.students.StudentResponse> pageContent = filteredAll.subList(from, to);
        Map<String,Object> resp = Map.of(
                "page", page,
                "size", size,
                "totalElements", total,
                "totalPages", (int) Math.ceil((double) total / (double) size),
                "data", pageContent
        );
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchStudents(
            @RequestParam(required = false) String q,
            @RequestParam(value = "classId", required = false) String classIdStr,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body(Map.of("error","Not authenticated"));
        String username = auth.getName();
        User currentUser = userRepository.findByUsername(username).orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "User not found"));
        boolean isAdmin = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_ADMIN"));
        boolean isTeacher = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_TEACHER"));
        boolean isStudent = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_STUDENT"));

        // fetch raw results
        List<com.example.English.Center.Data.dto.students.StudentResponse> all = studentService.searchStudentsRaw(q);

        // parse classId safely (frontend may send empty string)
        Long classId = null;
        if (classIdStr != null && !classIdStr.isBlank()) {
            try { classId = Long.parseLong(classIdStr); } catch (NumberFormatException nfe) {
                return ResponseEntity.badRequest().body(Map.of("error","classId must be a number"));
            }
        }

        // If classId specified, restrict to that class (verify membership unless admin)
        Set<Long> allowedStudentIds = null;
        if (classId != null) {
            var opt = classRepository.findById(classId);
            if (opt.isEmpty()) return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(Map.of("error","Class not found"));
            var classRoom = opt.get();
            boolean userInClass = false;
            if (isAdmin) userInClass = true;
            else {
                if (isStudent) {
                    var sOpt = studentRepository.findByUserId(currentUser.getId());
                    if (sOpt.isPresent() && classRoom.getStudents() != null) userInClass = classRoom.getStudents().stream().anyMatch(s -> s.getId().equals(sOpt.get().getId()));
                }
                if (!userInClass && isTeacher) {
                    var tOpt = teacherRepository.findByUserId(currentUser.getId());
                    if (tOpt.isPresent()) userInClass = classRoom.getTeacher() != null && classRoom.getTeacher().getId().equals(tOpt.get().getId());
                }
            }
            if (!userInClass && !isAdmin) return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(Map.of("error","Forbidden"));
            allowedStudentIds = new java.util.HashSet<>();
            if (classRoom.getStudents() != null) {
                for (var s : classRoom.getStudents()) {
                    allowedStudentIds.add(s.getId());
                }
            }
        }

        // If no classId and not admin, limit to students in user's classes
        if (allowedStudentIds == null && !isAdmin) {
            allowedStudentIds = new java.util.HashSet<>();
            // student: collect students from classes the student is in
            if (isStudent) {
                var sOpt = studentRepository.findByUserId(currentUser.getId());
                if (sOpt.isPresent()) {
                    var classes = classRepository.findByStudents_Id(sOpt.get().getId());
                    if (classes != null) {
                        for (var c : classes) {
                            if (c.getStudents() != null) {
                                for (var s : c.getStudents()) {
                                    allowedStudentIds.add(s.getId());
                                }
                            }
                        }
                    }
                }
            } else if (isTeacher) {
                // teacher: collect students from classes the teacher teaches
                var tOpt = teacherRepository.findByUserId(currentUser.getId());
                if (tOpt.isPresent()) {
                    var classes = classRepository.findByTeacher_Id(tOpt.get().getId());
                    if (classes != null) {
                        for (var c : classes) {
                            if (c.getStudents() != null) {
                                for (var s : c.getStudents()) {
                                    allowedStudentIds.add(s.getId());
                                }
                            }
                        }
                    }
                }
            }
            // note: admins already handled above
        }

        // Filter results if allowedStudentIds set is present
        // Make a final copy for lambda capture
        final java.util.Set<Long> allowedForFilter = (allowedStudentIds == null) ? null : java.util.Collections.unmodifiableSet(new java.util.HashSet<>(allowedStudentIds));
        List<com.example.English.Center.Data.dto.students.StudentResponse> filtered;
        if (allowedForFilter == null) filtered = all; else filtered = all.stream().filter(s -> s.getId()!=null && allowedForFilter.contains(s.getId())).toList();

        long total = filtered.size();
        int from = Math.min(page * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        List<com.example.English.Center.Data.dto.students.StudentResponse> pageContent = filtered.subList(from, to);
        Map<String,Object> resp = Map.of(
                "page", page,
                "size", size,
                "totalElements", total,
                "totalPages", (int) Math.ceil((double) total / (double) size),
                "data", pageContent
        );
        return ResponseEntity.ok(resp);
    }
}
