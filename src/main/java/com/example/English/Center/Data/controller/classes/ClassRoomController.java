package com.example.English.Center.Data.controller.classes;

import com.example.English.Center.Data.dto.classes.ClassRoomRequest;
import com.example.English.Center.Data.dto.classes.ClassRoomResponse;
import com.example.English.Center.Data.entity.classes.ClassRoom;
import com.example.English.Center.Data.entity.courses.Course;
import com.example.English.Center.Data.entity.teachers.Teacher;
import com.example.English.Center.Data.entity.classes.Room;
import com.example.English.Center.Data.entity.classes.FixedSchedule;
import com.example.English.Center.Data.entity.students.Student;
import com.example.English.Center.Data.repository.classes.ClassEntityRepository;
import com.example.English.Center.Data.repository.courses.CourseRepository;
import com.example.English.Center.Data.repository.teachers.TeacherRepository;
import com.example.English.Center.Data.repository.classes.RoomRepository;
import com.example.English.Center.Data.repository.classes.FixedScheduleRepository;
import com.example.English.Center.Data.repository.students.StudentRepository;
import com.example.English.Center.Data.service.classes.ClassRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.example.English.Center.Data.dto.classes.ClassRoomMapper;

@RestController @RequestMapping("/class-rooms")
public class ClassRoomController {
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

    @PostMapping
    public ResponseEntity<?> createClassRoom(@RequestBody ClassRoomRequest request) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
        FixedSchedule fixedSchedule = fixedScheduleRepository.findById(request.getFixedScheduleId())
                .orElseThrow(() -> new IllegalArgumentException("FixedSchedule not found"));
        int duration = course.getDuration();
        String daysOfWeek = fixedSchedule.getDaysOfWeek();
        java.time.LocalDate endDate = classRoomService.calculateEndDate(request.getStartDate(), daysOfWeek, duration);

        // Fetch students and validate
        List<Student> students = List.of();
        if (request.getStudentIds() != null && !request.getStudentIds().isEmpty()) {
            students = studentRepository.findAllById(request.getStudentIds());
            // detect missing ids
            List<Long> foundIds = students.stream().map(Student::getId).toList();
            List<Long> missing = request.getStudentIds().stream().filter(id -> !foundIds.contains(id)).toList();
            if (!missing.isEmpty()) {
                throw new IllegalArgumentException("Some studentIds not found: " + missing);
            }
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
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
        FixedSchedule fixedSchedule = fixedScheduleRepository.findById(request.getFixedScheduleId())
                .orElseThrow(() -> new IllegalArgumentException("FixedSchedule not found"));
        int duration = course.getDuration();
        String daysOfWeek = fixedSchedule.getDaysOfWeek();
        java.time.LocalDate endDate = classRoomService.calculateEndDate(request.getStartDate(), daysOfWeek, duration);

        // Fetch students and validate
        List<Student> students = List.of();
        if (request.getStudentIds() != null && !request.getStudentIds().isEmpty()) {
            students = studentRepository.findAllById(request.getStudentIds());
            List<Long> foundIds = students.stream().map(Student::getId).toList();
            List<Long> missing = request.getStudentIds().stream().filter(sid -> !foundIds.contains(sid)).toList();
            if (!missing.isEmpty()) {
                throw new IllegalArgumentException("Some studentIds not found: " + missing);
            }
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
}
