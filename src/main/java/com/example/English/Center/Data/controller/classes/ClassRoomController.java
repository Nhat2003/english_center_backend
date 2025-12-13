package com.example.English.Center.Data.controller.classes;

import com.example.English.Center.Data.dto.classes.ClassRoomMapper;
import com.example.English.Center.Data.dto.classes.ClassRoomRequest;
import com.example.English.Center.Data.dto.classes.ClassRoomResponse;
import com.example.English.Center.Data.entity.classes.ClassRoom;
import com.example.English.Center.Data.entity.classes.FixedSchedule;
import com.example.English.Center.Data.entity.classes.Room;
import com.example.English.Center.Data.entity.courses.Course;
import com.example.English.Center.Data.entity.students.Student;
import com.example.English.Center.Data.entity.teachers.Teacher;
import com.example.English.Center.Data.entity.users.User;
import com.example.English.Center.Data.repository.classes.ClassEntityRepository;
import com.example.English.Center.Data.repository.classes.FixedScheduleRepository;
import com.example.English.Center.Data.repository.classes.RoomRepository;
import com.example.English.Center.Data.repository.courses.CourseRepository;
import com.example.English.Center.Data.repository.students.StudentRepository;
import com.example.English.Center.Data.repository.teachers.TeacherRepository;
import com.example.English.Center.Data.repository.users.UserRepository;
import com.example.English.Center.Data.service.classes.ClassRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController @RequestMapping("/class-rooms")
public class ClassRoomController {
    private static final Logger logger = LoggerFactory.getLogger(ClassRoomController.class);

    @Autowired
    private ClassRoomService classRoomService;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private TeacherRepository teacherRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private FixedScheduleRepository fixedScheduleRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private ClassEntityRepository classEntityRepository;
    @Autowired
    private com.example.English.Center.Data.service.classes.ClassStudentService classStudentService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private com.example.English.Center.Data.service.classes.ClassSessionService classSessionService;

    @PostMapping
    public ResponseEntity<?> createClassRoom(@RequestBody ClassRoomRequest request) {
        var courseOpt = courseRepository.findById(request.getCourseId());
        if (courseOpt.isEmpty()) return ResponseEntity.badRequest().body("Course not found");
        var teacherOpt = teacherRepository.findById(request.getTeacherId());
        if (teacherOpt.isEmpty()) return ResponseEntity.badRequest().body("Teacher not found");
        var roomOpt = roomRepository.findById(request.getRoomId());
        if (roomOpt.isEmpty()) return ResponseEntity.badRequest().body("Room not found");
        var fsOpt = fixedScheduleRepository.findById(request.getFixedScheduleId());
        if (fsOpt.isEmpty()) return ResponseEntity.badRequest().body("FixedSchedule not found");
        Course course = courseOpt.get();
        Teacher teacher = teacherOpt.get();
        Room room = roomOpt.get();
        FixedSchedule fixedSchedule = fsOpt.get();
        int duration = course.getDuration();
        String daysOfWeek = fixedSchedule.getDaysOfWeek();
        java.time.LocalDate endDate = classRoomService.calculateEndDate(request.getStartDate(), daysOfWeek, duration);

        // Fetch students, deduplicate incoming ids, and validate
        Set<Student> students = Set.of();
        if (request.getStudentIds() != null && !request.getStudentIds().isEmpty()) {
            // preserve insertion order and remove duplicates
            List<Long> uniqueIds = request.getStudentIds().stream().distinct().collect(Collectors.toList());
            List<Student> found = studentRepository.findAllById(uniqueIds);
            Set<Long> foundIds = found.stream().map(Student::getId).collect(Collectors.toSet());
            List<Long> missing = uniqueIds.stream().filter(id -> !foundIds.contains(id)).toList();
            if (!missing.isEmpty()) {
                System.out.println("Missing studentIds when creating class: " + missing);
                return ResponseEntity.badRequest().body("Some studentIds not found: " + missing);
            }
            students = new LinkedHashSet<>(found);
        }

        System.out.println("Creating class with " + students.size() + " students");
        ClassRoom classRoom = ClassRoom.builder()
                .name(request.getName())
                .course(course)
                .teacher(teacher)
                .room(room)
                .fixedSchedule(fixedSchedule)
                .startDate(request.getStartDate())
                .endDate(endDate)
                .students(students)
                .build();
        ClassRoom saved = classRoomService.create(classRoom);
        System.out.println("Saved class id=" + saved.getId() + " with students count=" + (saved.getStudents() != null ? saved.getStudents().size() : 0));
        return ResponseEntity.ok(ClassRoomMapper.toResponse(saved));
    }

    @GetMapping
    public ResponseEntity<List<ClassRoomResponse>> getAllClassRooms() {
        List<ClassRoom> classRooms = classRoomService.getAll();
        List<ClassRoomResponse> responses = classRooms.stream()
            .map(ClassRoomMapper::toResponse)
            .toList();
        return ResponseEntity.ok(responses);
    }

    // New: get classes by student id
        @GetMapping("/by-student/{studentId}")
    public ResponseEntity<List<ClassRoomResponse>> getClassesByStudent(@PathVariable Long studentId) {
        List<ClassRoom> classRooms = classRoomService.getByStudentId(studentId);
        List<ClassRoomResponse> responses = classRooms.stream().map(ClassRoomMapper::toResponse).toList();
        return ResponseEntity.ok(responses);
    }

    // New: get classes by teacher id
    @GetMapping("/by-teacher/{teacherId}")
    public ResponseEntity<List<ClassRoomResponse>> getClassesByTeacher(@PathVariable Long teacherId) {
        List<ClassRoom> classRooms = classRoomService.getByTeacherId(teacherId);
        List<ClassRoomResponse> responses = classRooms.stream().map(ClassRoomMapper::toResponse).toList();
        return ResponseEntity.ok(responses);
    }

    // New: get classes for authenticated teacher
    @GetMapping("/me")
    public ResponseEntity<List<ClassRoomResponse>> getMyClasses() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return ResponseEntity.status(403).build();
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(403).build();
        var teacherOpt = teacherRepository.findByUserId(user.getId());
        if (teacherOpt.isEmpty()) return ResponseEntity.status(403).build();
        Long teacherId = teacherOpt.get().getId();
        List<ClassRoom> classRooms = classEntityRepository.findByTeacher_Id(teacherId);
        List<ClassRoomResponse> responses = classRooms.stream().map(ClassRoomMapper::toResponse).toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClassRoomResponse> getClassRoomById(@PathVariable Long id) {
        return classRoomService.getById(id)
            .map(classRoom -> ResponseEntity.ok(ClassRoomMapper.toResponse(classRoom)))
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClassRoom(@PathVariable Long id) {
        classRoomService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClassRoomResponse> updateClassRoom(@PathVariable Long id, @RequestBody ClassRoomRequest request) {
        var courseOpt = courseRepository.findById(request.getCourseId());
        if (courseOpt.isEmpty()) return ResponseEntity.badRequest().build();
        var teacherOpt = teacherRepository.findById(request.getTeacherId());
        if (teacherOpt.isEmpty()) return ResponseEntity.badRequest().build();
        var roomOpt = roomRepository.findById(request.getRoomId());
        if (roomOpt.isEmpty()) return ResponseEntity.badRequest().build();
        var fsOpt = fixedScheduleRepository.findById(request.getFixedScheduleId());
        if (fsOpt.isEmpty()) return ResponseEntity.badRequest().build();
        Course course = courseOpt.get();
        Teacher teacher = teacherOpt.get();
        Room room = roomOpt.get();
        FixedSchedule fixedSchedule = fsOpt.get();
        int duration = course.getDuration();
        String daysOfWeek = fixedSchedule.getDaysOfWeek();
        java.time.LocalDate endDate = classRoomService.calculateEndDate(request.getStartDate(), daysOfWeek, duration);

        // Fetch students, deduplicate incoming ids, and validate
        Set<Student> students = Set.of();
        if (request.getStudentIds() != null && !request.getStudentIds().isEmpty()) {
            List<Long> uniqueIds = request.getStudentIds().stream().distinct().collect(Collectors.toList());
            List<Student> found = studentRepository.findAllById(uniqueIds);
            Set<Long> foundIds = found.stream().map(Student::getId).collect(Collectors.toSet());
            List<Long> missing = uniqueIds.stream().filter(sid -> !foundIds.contains(sid)).toList();
            if (!missing.isEmpty()) {
                System.out.println("Missing studentIds when updating class id=" + id + ": " + missing);
                return ResponseEntity.badRequest().build();
            }
            students = new LinkedHashSet<>(found);
        }

        System.out.println("Updating class id=" + id + " with " + students.size() + " students");
        ClassRoom classRoom = ClassRoom.builder()
                .id(id)
                .name(request.getName())
                .course(course)
                .teacher(teacher)
                .room(room)
                .fixedSchedule(fixedSchedule)
                .startDate(request.getStartDate())
                .endDate(endDate)
                .students(students)
                .build();
        ClassRoom updated = classRoomService.update(id, classRoom);
        System.out.println("Updated class id=" + updated.getId() + " with students count=" + (updated.getStudents() != null ? updated.getStudents().size() : 0));
        return ResponseEntity.ok(ClassRoomMapper.toResponse(updated));
    }

    // Get students in a class (accessible by students, teachers, admin)
    @GetMapping("/{classRoomId}/students")
    public ResponseEntity<?> getStudentsInClass(@PathVariable Long classRoomId) {
        var classOpt = classEntityRepository.findById(classRoomId);
        if (classOpt.isEmpty()) return ResponseEntity.notFound().build();

        ClassRoom classRoom = classOpt.get();
        Set<Student> students = classRoom.getStudents();

        if (students == null || students.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        // Return list of student info
        List<java.util.Map<String, Object>> result = students.stream()
            .map(student -> {
                java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
                map.put("id", student.getId());
                map.put("fullName", student.getFullName());
                map.put("email", student.getEmail());
                map.put("phone", student.getPhone());
                map.put("address", student.getAddress());
                map.put("dob", student.getDob());
                map.put("gender", student.getGender());
                map.put("joinedAt", student.getJoinedAt());
                return map;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/{classId}/sessions/{date}/reschedule")
    public ResponseEntity<?> rescheduleSession(@PathVariable Long classId, @PathVariable java.time.LocalDate date, @RequestBody com.example.English.Center.Data.dto.classes.RescheduleRequest req) {
        try {
            // If originalDate provided in body, use it instead of path variable
            java.time.LocalDate originalDate = date;
            if (req.getOriginalDate() != null && !req.getOriginalDate().isEmpty()) {
                originalDate = java.time.LocalDate.parse(req.getOriginalDate());
            }

            var saved = classSessionService.rescheduleSession(classId, originalDate, req);
            var resp = new com.example.English.Center.Data.dto.classes.RescheduleResponse(
                    saved.getId(),
                    saved.getClassRoom() != null ? saved.getClassRoom().getId() : null,
                    saved.getOriginalDate() != null ? saved.getOriginalDate().toString() : null,
                    saved.getNewStart() != null ? saved.getNewStart().toString() : null,
                    saved.getNewEnd() != null ? saved.getNewEnd().toString() : null,
                    saved.getStatus() != null ? saved.getStatus().name() : null
            );
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(iae.getMessage());
        } catch (SecurityException se) {
            return ResponseEntity.status(403).body(se.getMessage());
        } catch (IllegalStateException ise) {
            return ResponseEntity.status(409).body(ise.getMessage());
        } catch (Exception ex) {
            logger.error("Error while rescheduling session", ex);
            return ResponseEntity.status(500).body("Internal server error");
        }
    }
}
