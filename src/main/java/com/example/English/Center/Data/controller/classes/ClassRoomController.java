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
import com.example.English.Center.Data.service.classes.ClassStudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.example.English.Center.Data.dto.classes.ClassRoomMapper;

@RestController
@RequestMapping("/class-rooms")
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
    private ClassStudentService classStudentService;

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
        ClassRoom classRoom = ClassRoom.builder()
                .name(request.getName())
                .course(course)
                .teacher(teacher)
                .room(room)
                .fixedSchedule(fixedSchedule)
                .startDate(request.getStartDate())
                .endDate(endDate)
                .build();
        ClassRoom savedClassRoom = classRoomService.create(classRoom);
        if (request.getStudentIds() != null) {
            for (Long studentId : request.getStudentIds()) {
                Student student = studentRepository.findById(studentId)
                        .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));
                classStudentService.create(savedClassRoom, student);
            }
        }
        // Map sang DTO bằng mapper
        ClassRoomResponse response = ClassRoomMapper.toResponse(savedClassRoom);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateClassRoom(@PathVariable Long id, @RequestBody ClassRoomRequest request) {
        ClassRoom classRoom = classRoomService.getById(id)
                .orElseThrow(() -> new IllegalArgumentException("ClassRoom not found"));
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

        classRoom.setName(request.getName());
        classRoom.setCourse(course);
        classRoom.setTeacher(teacher);
        classRoom.setRoom(room);
        classRoom.setFixedSchedule(fixedSchedule);
        classRoom.setStartDate(request.getStartDate());
        classRoom.setEndDate(endDate);

        ClassRoom updatedClassRoom = classRoomService.update(id, classRoom);
        ClassRoomResponse response = ClassRoomMapper.toResponse(updatedClassRoom);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ClassRoomResponse>> getAllClassRooms() {
        List<ClassRoom> classRooms = classRoomService.getAll();
        List<ClassRoomResponse> responses = classRooms.stream()
                .map(ClassRoomMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteClassRoom(@PathVariable Long id) {
        classRoomService.delete(id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/{id}")
    public ResponseEntity<ClassRoomResponse> getClassRoomById(@PathVariable Long id){
        ClassRoom classRoom = classRoomService.getById(id)
                .orElseThrow(() -> new IllegalArgumentException("ClassRoom not found"));
        ClassRoomResponse response = ClassRoomMapper.toResponse(classRoom);
        return ResponseEntity.ok(response);
    }

}

