package com.example.English.Center.Data.controller.students;

import com.example.English.Center.Data.dto.students.StudentRequest;
import com.example.English.Center.Data.dto.students.StudentResponse;
import com.example.English.Center.Data.dto.students.StudentDetailResponse;
import com.example.English.Center.Data.dto.SuccessResponse;
import com.example.English.Center.Data.dto.submission.SubmissionResponse;
import com.example.English.Center.Data.entity.attendance.Attendance;
import com.example.English.Center.Data.entity.students.Student;
import com.example.English.Center.Data.entity.teachers.Teacher;
import com.example.English.Center.Data.entity.users.User;
import com.example.English.Center.Data.service.students.StudentService;
import com.example.English.Center.Data.service.attendance.AttendanceService;
import com.example.English.Center.Data.service.submisssion.SubmissionService;
import com.example.English.Center.Data.repository.users.UserRepository;
import com.example.English.Center.Data.repository.teachers.TeacherRepository;
import com.example.English.Center.Data.repository.students.StudentRepository;
import com.example.English.Center.Data.repository.classes.ClassEntityRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
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

    public StudentController(StudentService studentService,
                             AttendanceService attendanceService,
                             SubmissionService submissionService,
                             UserRepository userRepository,
                             TeacherRepository teacherRepository,
                             StudentRepository studentRepository,
                             ClassEntityRepository classRepository) {
        this.studentService = studentService;
        this.attendanceService = attendanceService;
        this.submissionService = submissionService;
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.classRepository = classRepository;
    }

    @GetMapping
    public List<StudentResponse> getAllStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return studentService.getAllStudents(page, size);
    }

    @GetMapping("/{id}")
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
}
