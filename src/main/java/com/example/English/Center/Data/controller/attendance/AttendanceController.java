package com.example.English.Center.Data.controller.attendance;

import com.example.English.Center.Data.entity.attendance.Attendance;
import com.example.English.Center.Data.service.attendance.AttendanceService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @GetMapping
    public List<Attendance> getAll() {
        return attendanceService.getAll();
    }

    @GetMapping("/{id}")
    public Attendance getById(@PathVariable Long id) {
        return attendanceService.getById(id).orElse(null);
    }

    @GetMapping("/class/{classId}")
    public List<Attendance> getByClassAndDate(@PathVariable Long classId,
                                              @RequestParam("date") String dateString) {
        LocalDate date = LocalDate.parse(dateString);
        return attendanceService.getByClassAndDate(classId, date);
    }

    @GetMapping("/student/{studentId}")
    public List<Attendance> getByStudent(@PathVariable Long studentId) {
        return attendanceService.getByStudent(studentId);
    }

    @PostMapping
    public Attendance create(@RequestBody Attendance attendance) {
        return attendanceService.save(attendance);
    }

    @PutMapping("/{id}")
    public Attendance update(@PathVariable Long id, @RequestBody Attendance attendance) {
        attendance.setId(id);
        return attendanceService.save(attendance);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        attendanceService.delete(id);
    }
}