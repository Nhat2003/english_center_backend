package com.example.English.Center.Data.controller.teachers;

import com.example.English.Center.Data.dto.teachers.TeacherRequest;
import com.example.English.Center.Data.dto.teachers.TeacherResponse;
import com.example.English.Center.Data.service.teachers.TeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/teachers")
public class TeacherController {

    @Autowired
    private TeacherService teacherService;

    @GetMapping
    public List<TeacherResponse> getAllTeachers() {
        return teacherService.getAllTeachers();
    }

    @GetMapping("/{id}")
    public TeacherResponse getTeacherById(@PathVariable Long id) {
        return teacherService.getTeacherById(id);
    }

    @PostMapping
    public TeacherResponse createTeacher(@RequestBody TeacherRequest request) {
        return teacherService.createTeacher(request);
    }

    @PutMapping("/{id}")
    public TeacherResponse updateTeacher(@PathVariable Long id, @RequestBody TeacherRequest request) {
        return teacherService.updateTeacher(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteTeacher(@PathVariable Long id) {
        teacherService.deleteTeacher(id);
    }
}
