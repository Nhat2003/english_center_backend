package com.example.English.Center.Data.controller.students;

import com.example.English.Center.Data.dto.students.StudentRequest;
import com.example.English.Center.Data.dto.students.StudentResponse;
import com.example.English.Center.Data.dto.SuccessResponse;
import com.example.English.Center.Data.service.students.StudentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/students")
public class StudentController {
    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
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
