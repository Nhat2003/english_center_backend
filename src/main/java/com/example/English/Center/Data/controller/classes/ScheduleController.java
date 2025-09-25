package com.example.English.Center.Data.controller.classes;

import com.example.English.Center.Data.dto.classes.ScheduleRequest;
import com.example.English.Center.Data.dto.classes.ScheduleResponse;
import com.example.English.Center.Data.dto.SuccessResponse;
import com.example.English.Center.Data.service.classes.ScheduleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/schedules")
public class ScheduleController {
    private final ScheduleService scheduleService;
    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public List<ScheduleResponse> getAll() {
        return scheduleService.getAll();
    }

    @GetMapping("/{id}")
    public ScheduleResponse getById(@PathVariable Long id) {
        return scheduleService.getById(id);
    }

    @PostMapping
    public ResponseEntity<SuccessResponse> create(@RequestBody @Valid ScheduleRequest request) {
        scheduleService.create(request);
        return ResponseEntity.ok(new SuccessResponse("Schedule created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SuccessResponse> update(@PathVariable Long id, @RequestBody @Valid ScheduleRequest request) {
        scheduleService.update(id, request);
        return ResponseEntity.ok(new SuccessResponse("Schedule updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<SuccessResponse> delete(@PathVariable Long id) {
        scheduleService.delete(id);
        return ResponseEntity.ok(new SuccessResponse("Schedule deleted successfully"));
    }
}

