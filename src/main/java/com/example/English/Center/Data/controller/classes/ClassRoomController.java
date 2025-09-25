package com.example.English.Center.Data.controller.classes;

import com.example.English.Center.Data.dto.classes.ClassRoomRequest;
import com.example.English.Center.Data.dto.classes.ClassRoomResponse;
import com.example.English.Center.Data.dto.SuccessResponse;
import com.example.English.Center.Data.service.classes.ClassRoomService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/classes")
public class ClassRoomController {
    private final ClassRoomService classRoomService;
    public ClassRoomController(ClassRoomService classRoomService) {
        this.classRoomService = classRoomService;
    }

    @GetMapping
    public List<ClassRoomResponse> getAll() {
        return classRoomService.getAll();
    }

    @GetMapping("/{id}")
    public ClassRoomResponse getById(@PathVariable Long id) {
        return classRoomService.getById(id);
    }

    @PostMapping
    public ResponseEntity<SuccessResponse> create(@RequestBody @Valid ClassRoomRequest request) {
        classRoomService.create(request);
        return ResponseEntity.ok(new SuccessResponse("Class created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SuccessResponse> update(@PathVariable Long id, @RequestBody @Valid ClassRoomRequest request) {
        classRoomService.update(id, request);
        return ResponseEntity.ok(new SuccessResponse("Class updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<SuccessResponse> delete(@PathVariable Long id) {
        classRoomService.delete(id);
        return ResponseEntity.ok(new SuccessResponse("Class deleted successfully"));
    }
}

