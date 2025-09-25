package com.example.English.Center.Data.controller.courses;

import com.example.English.Center.Data.dto.courses.CourseRequest;
import com.example.English.Center.Data.dto.courses.CourseResponse;
import com.example.English.Center.Data.dto.SuccessResponse;
import com.example.English.Center.Data.service.courses.CourseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/courses")
public class CourseController {
    private final CourseService courseService;
    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public List<CourseResponse> getAll() {
        return courseService.getAll();
    }

    @GetMapping("/{id}")
    public CourseResponse getById(@PathVariable Long id) {
        return courseService.getById(id);
    }

    @PostMapping
    public ResponseEntity<SuccessResponse> create(@RequestBody @Valid CourseRequest request) {
        courseService.create(request);
        return ResponseEntity.ok(new SuccessResponse("Course created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SuccessResponse> update(@PathVariable Long id, @RequestBody @Valid CourseRequest request) {
        courseService.update(id, request);
        return ResponseEntity.ok(new SuccessResponse("Course updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<SuccessResponse> delete(@PathVariable Long id) {
        courseService.delete(id);
        return ResponseEntity.ok(new SuccessResponse("Course deleted successfully"));
    }
}

