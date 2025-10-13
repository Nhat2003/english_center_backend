package com.example.English.Center.Data.controller.classes;

import com.example.English.Center.Data.dto.classes.ClassStudentRequest;
import com.example.English.Center.Data.entity.classes.ClassRoom;
import com.example.English.Center.Data.entity.students.Student;
import com.example.English.Center.Data.entity.classes.ClassStudent;
import com.example.English.Center.Data.repository.classes.ClassEntityRepository;
import com.example.English.Center.Data.repository.students.StudentRepository;
import com.example.English.Center.Data.service.classes.ClassStudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/class-students")
public class ClassStudentController {
    @Autowired
    private ClassStudentService classStudentService;
    @Autowired
    private ClassEntityRepository classEntityRepository;
    @Autowired
    private StudentRepository studentRepository;

    @PostMapping
    public ResponseEntity<?> addStudentToClass(@RequestBody ClassStudentRequest request) {
        ClassRoom classRoom = classEntityRepository.findById(request.getClassRoomId())
                .orElseThrow(() -> new IllegalArgumentException("ClassRoom not found"));
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        ClassStudent classStudent = classStudentService.create(classRoom, student);
        return ResponseEntity.ok(classStudent);
    }
}

