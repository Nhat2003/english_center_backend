package com.example.English.Center.Data.controller.attendance;

import com.example.English.Center.Data.dto.attendance.AttendanceItemDto;
import com.example.English.Center.Data.dto.attendance.AttendanceSessionDto;
import com.example.English.Center.Data.dto.attendance.AttendanceSessionTodayRequest;
import com.example.English.Center.Data.entity.attendance.Attendance;
import com.example.English.Center.Data.entity.classes.ClassRoom;
import com.example.English.Center.Data.entity.students.Student;
import com.example.English.Center.Data.entity.teachers.Teacher;
import com.example.English.Center.Data.entity.users.User;
import com.example.English.Center.Data.repository.classes.ClassEntityRepository;
import com.example.English.Center.Data.repository.students.StudentRepository;
import com.example.English.Center.Data.repository.teachers.TeacherRepository;
import com.example.English.Center.Data.repository.users.UserRepository;
import com.example.English.Center.Data.service.attendance.AttendanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final ClassEntityRepository classRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;

    public AttendanceController(AttendanceService attendanceService,
                                ClassEntityRepository classRepository,
                                StudentRepository studentRepository,
                                UserRepository userRepository,
                                TeacherRepository teacherRepository) {
        this.attendanceService = attendanceService;
        this.classRepository = classRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
    }

    @GetMapping
    public List<Attendance> getAll() {
        return attendanceService.getAll();
    }

    @GetMapping("/{id}")
    public Attendance getById(@PathVariable Long id) {
        return attendanceService.getById(id).orElse(null);
    }

    @GetMapping("/class/{classId}")
    public List<Attendance> getByClassAndDate(@PathVariable Long classId,
                                              @RequestParam("date") String dateString) {
        LocalDate date = LocalDate.parse(dateString);
        return attendanceService.getByClassAndDate(classId, date);
    }

    // Students can view their own attendance; teachers/admins can view any student's attendance
    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getByStudent(@PathVariable Long studentId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authenticated");
        }

        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User not found"));

        boolean isAdmin = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_ADMIN"));
        boolean isTeacher = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_TEACHER"));
        boolean isStudent = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_STUDENT"));

        if (isAdmin) {
            // allowed for admins
        } else if (isTeacher) {
            // teacher may view only students in their classes
            Teacher teacher = teacherRepository.findByUserId(user.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher profile not found for user"));
            boolean teachesStudent = classRepository.findByTeacher_Id(teacher.getId())
                    .stream()
                    .flatMap(c -> c.getStudents().stream())
                    .anyMatch(s -> s.getId().equals(studentId));
            if (!teachesStudent) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher can only view attendance of students in their classes");
            }
        } else if (isStudent) {
            // ensure the user corresponds to this studentId
            Student student = studentRepository.findByUserId(user.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Student profile not found for user"));
            if (!student.getId().equals(studentId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Students can only view their own attendance");
            }
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
        }

        List<Attendance> attendances = attendanceService.getByStudent(studentId);
        return ResponseEntity.ok(attendances);
    }

    // New endpoint: authenticated student can fetch their own attendance without providing studentId
    @GetMapping("/me")
    public ResponseEntity<?> getMyAttendance() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authenticated");
        }
        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User not found"));

        // Find student profile for this user
        Student student = studentRepository.findByUserId(user.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found for user"));

        List<Attendance> attendances = attendanceService.getByStudent(student.getId());
        return ResponseEntity.ok(attendances);
    }

    @PostMapping
    public Attendance create(@RequestBody Attendance attendance) {
        return attendanceService.save(attendance);
    }

    @PutMapping("/{id}")
    public Attendance update(@PathVariable Long id, @RequestBody Attendance attendance) {
        attendance.setId(id);
        return attendanceService.save(attendance);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        attendanceService.delete(id);
    }

    // Create a session (bulk attendance records for a class session)
    @PostMapping("/session")
    public ResponseEntity<?> createSession(@Valid @RequestBody AttendanceSessionDto sessionDto) {
        Long classId = sessionDto.getClassId();
        var optClass = classRepository.findById(classId);
        if (optClass.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Class with id " + classId + " not found"));
        }
        var classRoom = optClass.get();

        // Authorization: only ADMIN or the assigned teacher can create session
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authenticated");
        }
        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User not found"));
        boolean isAdmin = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_ADMIN"));
        boolean isTeacher = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_TEACHER"));
        if (!isAdmin) {
            if (!isTeacher) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only teachers or admins can create attendance sessions");
            }
            Teacher teacher = teacherRepository.findByUserId(user.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher profile not found for user"));
            if (classRoom.getTeacher() == null || !classRoom.getTeacher().getId().equals(teacher.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the assigned teacher can create sessions for this class");
            }
        }

        LocalDate date = sessionDto.getSessionDate();
        // 1) Validate date within class start/end
        if (date.isBefore(classRoom.getStartDate()) || date.isAfter(classRoom.getEndDate())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Ngày điểm danh phải nằm trong khoảng thời gian của lớp",
                                 "startDate", classRoom.getStartDate(),
                                 "endDate", classRoom.getEndDate(),
                                 "sessionDate", date));
        }
        // NOTE: Previously we enforced that the session date must match the class' fixed schedule daysOfWeek.
        // Requirement changed: allow teachers/admins to create attendance for any session date (not only scheduled days).
        // Therefore the days-of-week validation was removed so sessions can be created for any date within class range.

        // 3) Enforce session count limit equals course duration (number of meetings) and prevent duplicate on POST
        int plannedSessions = classRoom.getCourse() != null ? classRoom.getCourse().getDuration() : 0;
        if (plannedSessions > 0) {
            long recordedDistinct = attendanceService.countDistinctSessions(classId);
            boolean sessionExists = attendanceService.sessionExists(classId, date);
            long nextCount = recordedDistinct + (sessionExists ? 0 : 1);
            if (nextCount > plannedSessions) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Số buổi điểm danh sẽ vượt quá số buổi học dự kiến",
                                     "plannedSessions", plannedSessions,
                                     "recordedDistinctSessions", recordedDistinct,
                                     "sessionDate", date));
            }
            if (sessionExists) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Đã tồn tại buổi điểm danh cho ngày này. Hãy dùng PUT /attendance/session để thay thế.",
                                     "classId", classId,
                                     "sessionDate", date));
            }
        }

        // Validate students: exist and belong to class
        List<Long> payloadStudentIds = sessionDto.getItems().stream().map(AttendanceItemDto::getStudentId).distinct().collect(Collectors.toList());

        List<Long> missingStudents = payloadStudentIds.stream()
                .filter(id -> !studentRepository.existsById(id))
                .collect(Collectors.toList());
        if (!missingStudents.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Some students not found", "missingStudentIds", missingStudents));
        }

        // check membership
        var classStudentIds = classRoom.getStudents().stream().map(Student::getId).collect(Collectors.toSet());
        List<Long> notInClass = payloadStudentIds.stream().filter(id -> !classStudentIds.contains(id)).collect(Collectors.toList());
        if (!notInClass.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Some students are not in the class", "notInClass", notInClass));
        }

        List<Attendance> list = new ArrayList<>();
        for (AttendanceItemDto item : sessionDto.getItems()) {
            Attendance a = new Attendance();
            a.setSessionDate(date);
            a.setClassRoom(classRoom);
            Student s = new Student();
            s.setId(item.getStudentId());
            a.setStudent(s);
            a.setStatus(item.getStatus());
            a.setNote(item.getNote());
            list.add(a);
        }
        List<Attendance> saved = attendanceService.saveSession(list);
        // Build summary counts
        long present = saved.stream().filter(a -> a.getStatus() != null && a.getStatus().name().equals("PRESENT")).count();
        long absent = saved.stream().filter(a -> a.getStatus() != null && a.getStatus().name().equals("ABSENT")).count();
        long late = saved.stream().filter(a -> a.getStatus() != null && a.getStatus().name().equals("LATE")).count();

        Map<String, Object> response = Map.of(
                "attendance", saved,
                "summary", Map.of("present", present, "absent", absent, "late", late)
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Convenience: create or attempt to create attendance session for today
    @PostMapping("/session/today")
    public ResponseEntity<?> createTodaySession(@Valid @RequestBody AttendanceSessionTodayRequest sessionReq) {
        AttendanceSessionDto dto = new AttendanceSessionDto(sessionReq.getClassId(), LocalDate.now(), sessionReq.getItems());
        return createSession(dto);
    }

    // Replace an existing session (bulk update)
    @PutMapping("/session")
    public ResponseEntity<?> replaceSession(@Valid @RequestBody AttendanceSessionDto sessionDto) {
        Long classId = sessionDto.getClassId();
        var optClass = classRepository.findById(classId);
        if (optClass.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Class with id " + classId + " not found"));
        }
        var classRoom = optClass.get();

        // Authorization: only ADMIN or the assigned teacher can replace session
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authenticated");
        }
        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User not found"));
        boolean isAdmin = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_ADMIN"));
        boolean isTeacher = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_TEACHER"));
        if (!isAdmin) {
            if (!isTeacher) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only teachers or admins can replace attendance sessions");
            }
            Teacher teacher = teacherRepository.findByUserId(user.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher profile not found for user"));
            if (classRoom.getTeacher() == null || !classRoom.getTeacher().getId().equals(teacher.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the assigned teacher can replace sessions for this class");
            }
        }

        LocalDate date = sessionDto.getSessionDate();
        // basic validation: ensure date within class range
        if (date.isBefore(classRoom.getStartDate()) || date.isAfter(classRoom.getEndDate())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Ngày điểm danh phải nằm trong khoảng thời gian của lớp",
                            "startDate", classRoom.getStartDate(),
                            "endDate", classRoom.getEndDate(),
                            "sessionDate", date));
        }

        // Validate students exist and belong to class
        List<Long> payloadStudentIds = sessionDto.getItems().stream().map(AttendanceItemDto::getStudentId).distinct().collect(Collectors.toList());
        List<Long> missingStudents = payloadStudentIds.stream()
                .filter(id -> !studentRepository.existsById(id))
                .collect(Collectors.toList());
        if (!missingStudents.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Some students not found", "missingStudentIds", missingStudents));
        }
        var classStudentIds = classRoom.getStudents().stream().map(Student::getId).collect(Collectors.toSet());
        List<Long> notInClass = payloadStudentIds.stream().filter(id -> !classStudentIds.contains(id)).collect(Collectors.toList());
        if (!notInClass.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Some students are not in the class", "notInClass", notInClass));
        }

        List<Attendance> list = new ArrayList<>();
        for (AttendanceItemDto item : sessionDto.getItems()) {
            Attendance a = new Attendance();
            a.setSessionDate(date);
            a.setClassRoom(classRoom);
            Student s = new Student();
            s.setId(item.getStudentId());
            a.setStudent(s);
            a.setStatus(item.getStatus());
            a.setNote(item.getNote());
            list.add(a);
        }

        List<Attendance> saved = attendanceService.replaceSession(classId, date, list);

        long present = saved.stream().filter(a -> a.getStatus() != null && a.getStatus().name().equals("PRESENT")).count();
        long absent = saved.stream().filter(a -> a.getStatus() != null && a.getStatus().name().equals("ABSENT")).count();
        long late = saved.stream().filter(a -> a.getStatus() != null && a.getStatus().name().equals("LATE")).count();

        Map<String, Object> response = Map.of(
                "attendance", saved,
                "summary", Map.of("present", present, "absent", absent, "late", late)
        );

        return ResponseEntity.ok(response);
    }

    // --- helpers ---
    private Set<Integer> parseDaysOfWeek(String daysOfWeek) {
        Set<Integer> set = new HashSet<>();
        if (daysOfWeek == null || daysOfWeek.trim().isEmpty()) return set;
        String[] parts = daysOfWeek.split(",");
        for (String p : parts) {
            try {
                set.add(Integer.parseInt(p.trim()));
            } catch (NumberFormatException ignored) {}
        }
        return set;
    }
}
