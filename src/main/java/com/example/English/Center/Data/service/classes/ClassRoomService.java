package com.example.English.Center.Data.service.classes;

import com.example.English.Center.Data.dto.classes.ClassRoomRequest;
import com.example.English.Center.Data.dto.classes.ClassRoomResponse;
import com.example.English.Center.Data.entity.classes.ClassRoom;
import com.example.English.Center.Data.entity.classes.Schedule;
import com.example.English.Center.Data.entity.students.Student;
import com.example.English.Center.Data.entity.teachers.Teacher;
import com.example.English.Center.Data.repository.classes.ClassRoomRepository;
import com.example.English.Center.Data.repository.classes.ScheduleRepository;
import com.example.English.Center.Data.repository.students.StudentRepository;
import com.example.English.Center.Data.repository.teachers.TeacherRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClassRoomService {
    private final ClassRoomRepository classRoomRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final ScheduleRepository scheduleRepository;

    public ClassRoomService(ClassRoomRepository classRoomRepository, TeacherRepository teacherRepository, StudentRepository studentRepository, ScheduleRepository scheduleRepository) {
        this.classRoomRepository = classRoomRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.scheduleRepository = scheduleRepository;
    }

    public List<ClassRoomResponse> getAll() {
        return classRoomRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public ClassRoomResponse getById(Long id) {
        ClassRoom c = classRoomRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Class not found"));
        return toResponse(c);
    }

    public ClassRoomResponse create(ClassRoomRequest request) {
        Teacher teacher = teacherRepository.findById(request.getTeacherId()).orElseThrow(() -> new EntityNotFoundException("Teacher not found"));
        Schedule schedule = scheduleRepository.findById(request.getScheduleId()).orElseThrow(() -> new EntityNotFoundException("Schedule not found"));
        List<Student> students = request.getStudentIds() == null ? List.of() : studentRepository.findAllById(request.getStudentIds());
        ClassRoom c = ClassRoom.builder()
                .name(request.getName())
                .schedule(schedule)
                .teacher(teacher)
                .students(students)
                .build();
        return toResponse(classRoomRepository.save(c));
    }

    public ClassRoomResponse update(Long id, ClassRoomRequest request) {
        ClassRoom c = classRoomRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Class not found"));
        Teacher teacher = teacherRepository.findById(request.getTeacherId()).orElseThrow(() -> new EntityNotFoundException("Teacher not found"));
        Schedule schedule = scheduleRepository.findById(request.getScheduleId()).orElseThrow(() -> new EntityNotFoundException("Schedule not found"));
        List<Student> students = request.getStudentIds() == null ? List.of() : studentRepository.findAllById(request.getStudentIds());
        c.setName(request.getName());
        c.setSchedule(schedule);
        c.setTeacher(teacher);
        c.setStudents(students);
        return toResponse(classRoomRepository.save(c));
    }

    public void delete(Long id) {
        classRoomRepository.deleteById(id);
    }

    private ClassRoomResponse toResponse(ClassRoom c) {
        ClassRoomResponse res = new ClassRoomResponse();
        res.setId(c.getId());
        res.setName(c.getName());
        res.setScheduleId(c.getSchedule() != null ? c.getSchedule().getId() : null);
        res.setScheduleName(c.getSchedule() != null ? c.getSchedule().getName() : null);
        res.setScheduleDescription(c.getSchedule() != null ? c.getSchedule().getDescription() : null);
        res.setTeacherId(c.getTeacher() != null ? c.getTeacher().getId() : null);
        res.setTeacherName(c.getTeacher() != null ? c.getTeacher().getFullName() : null);
        res.setStudentIds(c.getStudents() != null ? c.getStudents().stream().map(Student::getId).collect(Collectors.toList()) : List.of());
        res.setStudentNames(c.getStudents() != null ? c.getStudents().stream().map(Student::getFullName).collect(Collectors.toList()) : List.of());
        return res;
    }
}
