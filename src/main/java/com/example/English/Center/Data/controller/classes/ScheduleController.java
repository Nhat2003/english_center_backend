package com.example.English.Center.Data.controller.classes;

import com.example.English.Center.Data.dto.classes.ScheduleItemDTO;
import com.example.English.Center.Data.dto.classes.FixedScheduleDTO;
import com.example.English.Center.Data.entity.classes.FixedSchedule;
import com.example.English.Center.Data.entity.classes.Room;
import com.example.English.Center.Data.service.classes.FixedScheduleService;
import com.example.English.Center.Data.service.classes.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/schedule")
public class ScheduleController {
    @Autowired
    private FixedScheduleService fixedScheduleService;

    @Autowired
    private RoomService roomService;

    @GetMapping("/student/{studentId}")
    public List<ScheduleItemDTO> getStudentSchedule(@PathVariable Long studentId) {
        // TODO: Truy vấn các lớp mà học sinh tham gia, sinh lịch học thực tế
        return new ArrayList<>();
    }

    @GetMapping("/class/{classId}")
    public List<ScheduleItemDTO> getClassSchedule(@PathVariable Long classId) {
        // TODO: Truy vấn lớp học theo classId, sinh lịch học thực tế
        return new ArrayList<>();
    }

    @GetMapping("/teacher/{teacherId}")
    public List<ScheduleItemDTO> getTeacherSchedule(@PathVariable Long teacherId) {
        // TODO: Truy vấn các lớp mà giáo viên dạy, sinh lịch dạy thực tế
        return new ArrayList<>();
    }

    @GetMapping("/fixed")
    public List<FixedSchedule> getFixedSchedules() {
        return fixedScheduleService.getAll();
    }

    @PostMapping("/fixed")
    public FixedSchedule createFixedSchedule(@RequestBody FixedSchedule fixedSchedule) {
        return fixedScheduleService.create(fixedSchedule);
    }

    @PutMapping("/fixed/{id}")
    public FixedSchedule updateFixedSchedule(@PathVariable Long id, @RequestBody FixedSchedule fixedSchedule) {
        return fixedScheduleService.update(id, fixedSchedule);
    }

    @DeleteMapping("/fixed/{id}")
    public void deleteFixedSchedule(@PathVariable Long id) {
        fixedScheduleService.delete(id);
    }

    @GetMapping("/fixed/{id}")
    public ResponseEntity<FixedSchedule> getFixedScheduleById(@PathVariable Long id) {
        Optional<FixedSchedule> fixedSchedule = fixedScheduleService.getById(id);
        return fixedSchedule.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/class/{classId}/generated")
    public List<ScheduleItemDTO> generateClassSchedule(
            @PathVariable Long classId,
            @RequestParam String startDate, // yyyy-MM-dd
            @RequestParam String endDate,   // yyyy-MM-dd
            @RequestParam String daysOfWeek, // ví dụ: "2,4,6"
            @RequestParam String startTime,  // ví dụ: "18:00:00"
            @RequestParam String endTime,    // ví dụ: "20:00:00"
            @RequestParam(required = false) Integer teacherId,
            @RequestParam(required = false) Integer roomId
    ) {
        List<ScheduleItemDTO> result = new ArrayList<>();
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        Set<Integer> days = new HashSet<>();
        for (String d : daysOfWeek.split(",")) {
            days.add(Integer.parseInt(d));
        }
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        DateTimeFormatter isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
        ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");
        int session = 1;
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            int dayOfWeek = date.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday
            if (days.contains(dayOfWeek)) {
                LocalTime st = LocalTime.parse(startTime, timeFormatter);
                LocalTime et = LocalTime.parse(endTime, timeFormatter);
                ZonedDateTime startDateTime = ZonedDateTime.of(date, st, zoneId);
                ZonedDateTime endDateTime = ZonedDateTime.of(date, et, zoneId);
                String id = "class-" + classId + "-" + date;
                String title = "Buổi " + session + " - Lớp " + classId;
                result.add(new ScheduleItemDTO(
                    id,
                    title,
                    startDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    endDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    classId.intValue(),
                    teacherId,
                    roomId
                ));
                session++;
            }
        }
        return result;
    }

    @GetMapping("/student/{studentId}/generated")
    public List<ScheduleItemDTO> generateStudentSchedule(@PathVariable Long studentId) {
        List<Map<String, Object>> classes = new ArrayList<>();

        List<ScheduleItemDTO> result = new ArrayList<>();
        for (Map<String, Object> cls : classes) {
            result.addAll(generateClassSchedule(
                (Long) cls.get("classId"),
                (String) cls.get("startDate"),
                (String) cls.get("endDate"),
                (String) cls.get("daysOfWeek"),
                (String) cls.get("startTime"),
                (String) cls.get("endTime"),
                (Integer) cls.get("teacherId"),
                (Integer) cls.get("roomId")
            ));
        }
        return result;
    }

    @GetMapping("/room")
    public List<Room> getRooms() {
        return roomService.getAll();
    }

    @GetMapping("/room/{id}")
    public ResponseEntity<Room> getRoomById(@PathVariable Long id) {
        Optional<Room> room = roomService.getById(id);
        return room.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/room")
    public Room createRoom(@RequestBody Room room) {
        return roomService.create(room);
    }

    @PutMapping("/room/{id}")
    public Room updateRoom(@PathVariable Long id, @RequestBody Room room) {
        return roomService.update(id, room);
    }

    @DeleteMapping("/room/{id}")
    public void deleteRoom(@PathVariable Long id) {
        roomService.delete(id);
    }

}
