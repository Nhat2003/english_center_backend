package com.example.English.Center.Data.controller.classes;

import com.example.English.Center.Data.dto.classes.FixedScheduleDTO;
import com.example.English.Center.Data.dto.classes.ScheduleItemDTO;
import com.example.English.Center.Data.dto.classes.ScheduleItemForStudentDTO;
import com.example.English.Center.Data.dto.classes.ScheduleItemForTeacherDTO;
import com.example.English.Center.Data.entity.classes.ClassRoom;
import com.example.English.Center.Data.entity.classes.FixedSchedule;
import com.example.English.Center.Data.entity.classes.Room;
import com.example.English.Center.Data.entity.classes.Schedule;
import com.example.English.Center.Data.repository.classes.ClassEntityRepository;
import com.example.English.Center.Data.repository.classes.ScheduleRepository;
import com.example.English.Center.Data.service.classes.FixedScheduleService;
import com.example.English.Center.Data.service.classes.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/schedule")
public class ScheduleController {
    @Autowired
    private FixedScheduleService fixedScheduleService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ClassEntityRepository classRepository;

    @GetMapping("/student/{studentId}")
    public List<ScheduleItemForStudentDTO> getStudentSchedule(
            @PathVariable Long studentId,
            @RequestParam(required = false) String from, // yyyy-MM-dd
            @RequestParam(required = false) String to    // yyyy-MM-dd
    ) {
        LocalDate startDate = (from == null || from.isEmpty()) ? LocalDate.now().with(DayOfWeek.MONDAY) : LocalDate.parse(from);
        LocalDate endDate = (to == null || to.isEmpty()) ? startDate.plusDays(6) : LocalDate.parse(to);

        List<ClassRoom> classes = classRepository.findByStudents_Id(studentId);
        List<ScheduleItemDTO> result = new ArrayList<>();
        // dedupe ngay khi thêm: theo dõi các key đã thấy
        Set<String> seen = new HashSet<>();
        for (ClassRoom cls : classes) {
            List<ScheduleItemDTO> occ = generateOccurrencesForClassAndStudent(cls, studentId, startDate, endDate);
            for (ScheduleItemDTO dto : occ) {
                String key = makeKey(dto);
                if (seen.add(key)) {
                    result.add(dto);
                }
            }
        }
        result.sort(Comparator.comparing(ScheduleItemDTO::getStart));
        // Mapping sang DTO cho student
        List<ScheduleItemForStudentDTO> mapped = new ArrayList<>();
        for (ScheduleItemDTO dto : result) {
            mapped.add(mapToStudentDTO(dto));
        }
        return mapped;
    }



    @GetMapping("/teacher/{teacherId}")
    public List<ScheduleItemForTeacherDTO> getTeacherSchedule(@PathVariable Long teacherId,
                                                    @RequestParam(required = false) String from,
                                                    @RequestParam(required = false) String to) {
        LocalDate startDate = (from == null || from.isEmpty()) ? LocalDate.now().with(DayOfWeek.MONDAY) : LocalDate.parse(from);
        LocalDate endDate = (to == null || to.isEmpty()) ? startDate.plusDays(6) : LocalDate.parse(to);

        // find classes taught by teacher
        List<ClassRoom> classes = classRepository.findAll();
        List<ScheduleItemDTO> result = new ArrayList<>();
        // dedupe ngay khi thêm
        Set<String> seen = new HashSet<>();
        for (ClassRoom cls : classes) {
            if (cls.getTeacher() != null && Objects.equals(cls.getTeacher().getId(), teacherId)) {
                List<ScheduleItemDTO> occ = generateOccurrencesForClassAndStudent(cls, null, startDate, endDate);
                for (ScheduleItemDTO dto : occ) {
                    String key = makeKey(dto);
                    if (seen.add(key)) {
                        result.add(dto);
                    }
                }
            }
        }
        result.sort(Comparator.comparing(ScheduleItemDTO::getStart));
        // Mapping sang DTO cho teacher
        List<ScheduleItemForTeacherDTO> mapped = new ArrayList<>();
        for (ScheduleItemDTO dto : result) {
            mapped.add(mapToTeacherDTO(dto));
        }
        return mapped;
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

    // Helpers: convert persisted Schedule rows to DTOs (kept for compatibility)
    private List<ScheduleItemDTO> toDTOs(List<Schedule> schedules) {
        List<ScheduleItemDTO> result = new ArrayList<>();
        for (Schedule s : schedules) {
            Integer classId = s.getClassRoom() != null && s.getClassRoom().getId() != null ? s.getClassRoom().getId().intValue() : null;
            Integer teacherId = s.getTeacher() != null && s.getTeacher().getId() != null ? s.getTeacher().getId().intValue() : null;
            Integer roomId = s.getRoom() != null && s.getRoom().getId() != null ? s.getRoom().getId().intValue() : null;
            String start = s.getStartDateTime() != null ? s.getStartDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) : null;
            String end = s.getEndDateTime() != null ? s.getEndDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) : null;
            String date = s.getStartDateTime() != null ? s.getStartDateTime().toLocalDate().toString() : null; // new
            ScheduleItemDTO dto = new ScheduleItemDTO();
            dto.setId(s.getName());
            dto.setTitle(s.getTitle());
            dto.setStart(start);
            dto.setEnd(end);
            dto.setDate(date);
            dto.setClassId(classId);
            dto.setTeacherId(teacherId);
            dto.setRoomId(roomId);
            dto.setClassName(s.getClassRoom() != null ? s.getClassRoom().getName() : null);
            dto.setTeacherName(s.getTeacher() != null ? s.getTeacher().getFullName() : null);
            dto.setRoomName(s.getRoom() != null ? s.getRoom().getName() : null);
            dto.setStudentId(s.getStudent() != null ? s.getStudent().getId().intValue() : null);
            result.add(dto);
         }
         return result;
    }

    // New helpers: generate occurrences for a ClassRoom between from..to for an optional student
    private List<ScheduleItemDTO> generateOccurrencesForClassAndStudent(ClassRoom cls, Long studentId, LocalDate from, LocalDate to) {
        List<ScheduleItemDTO> result = new ArrayList<>();
        if (cls == null || cls.getFixedSchedule() == null || cls.getCourse() == null) return result;
        FixedSchedule fs = cls.getFixedSchedule();
        Set<Integer> days = parseDaysOfWeek(fs.getDaysOfWeek());
        if (days.isEmpty()) return result;
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        LocalTime st = LocalTime.parse(fs.getStartTime(), timeFormatter);
        LocalTime et = LocalTime.parse(fs.getEndTime(), timeFormatter);
        ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");

        LocalDate classStart = cls.getStartDate();
        LocalDate classEnd = cls.getEndDate();
        if (from.isBefore(classStart)) from = classStart;
        if (to.isAfter(classEnd)) to = classEnd;
        if (from.isAfter(to)) return result;

        int totalSessions = cls.getCourse().getDuration();
        int offset = countSessionsBeforeDate(cls, days, from);
        int sessionCount = offset;

        // Chuẩn bị danh sách học sinh nếu là lịch giáo viên
        List<ScheduleItemDTO.StudentInfo> studentInfos = null;
        if (studentId == null && cls.getStudents() != null) {
            studentInfos = cls.getStudents().stream()
                .map(s -> new ScheduleItemDTO.StudentInfo(s.getId(), s.getUser() != null ? s.getUser().getFullName() : null))
                .toList();
        }

        for (LocalDate d = from; !d.isAfter(to) && sessionCount < totalSessions; d = d.plusDays(1)) {
            if (days.contains(d.getDayOfWeek().getValue()) && !d.isBefore(classStart)) {
                sessionCount++;
                String id = "class-" + cls.getId() + "-" + d;
                String title = "Buổi " + sessionCount + " - " + cls.getName();
                ZonedDateTime startZ = ZonedDateTime.of(d, st, zoneId);
                ZonedDateTime endZ = ZonedDateTime.of(d, et, zoneId);
                ScheduleItemDTO dto = new ScheduleItemDTO();
                dto.setId(id);
                dto.setTitle(title);
                dto.setStart(startZ.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                dto.setEnd(endZ.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                dto.setDate(d.toString()); // new: set specific date
                dto.setClassId(cls.getId() != null ? cls.getId().intValue() : null);
                dto.setTeacherId(cls.getTeacher() != null ? cls.getTeacher().getId().intValue() : null);
                dto.setRoomId(cls.getRoom() != null ? cls.getRoom().getId().intValue() : null);
                dto.setClassName(cls.getName());
                dto.setCourseName(cls.getCourse() != null ? cls.getCourse().getName() : null);
                // Nếu là lịch giáo viên thì không set teacherName, set students
                if (studentId == null) {
                    dto.setTeacherName(null);
                    dto.setStudents(studentInfos);
                } else {
                    dto.setTeacherName(cls.getTeacher() != null ? cls.getTeacher().getFullName() : null);
                    dto.setStudents(null);
                }
                dto.setRoomName(cls.getRoom() != null ? cls.getRoom().getName() : null);
                dto.setStudentId(studentId != null ? studentId.intValue() : null);
                dto.setSessionIndex(sessionCount);
                dto.setTotalSessions(totalSessions);
                dto.setFixedSchedule(new FixedScheduleDTO(fs.getName(), fs.getDaysOfWeek(), fs.getStartTime(), fs.getEndTime()));
                result.add(dto);
            }
        }
        return result;
    }

    private Set<Integer> parseDaysOfWeek(String daysOfWeek) {
        Set<Integer> set = new HashSet<>();
        if (daysOfWeek == null || daysOfWeek.trim().isEmpty()) return set;
        String[] parts = daysOfWeek.split(",");
        for (String p : parts) {
            try {
                int v = Integer.parseInt(p.trim());
                set.add(v); // keep using 1=Monday..7=Sunday
            } catch (NumberFormatException ignored) {}
        }
        return set;
    }

    private int countSessionsBeforeDate(ClassRoom cls, Set<Integer> days, LocalDate dateExclusive) {
        LocalDate cur = cls.getStartDate();
        int count = 0;
        while (cur.isBefore(dateExclusive) && (cls.getCourse() == null || count < cls.getCourse().getDuration())) {
            if (days.contains(cur.getDayOfWeek().getValue())) count++;
            cur = cur.plusDays(1);
        }
        return count;
    }

    // Mapping helpers
    private ScheduleItemForTeacherDTO mapToTeacherDTO(ScheduleItemDTO dto) {
        ScheduleItemForTeacherDTO t = new ScheduleItemForTeacherDTO();
        t.setId(dto.getId());
        t.setTitle(dto.getTitle());
        t.setStart(dto.getStart());
        t.setEnd(dto.getEnd());
        t.setDate(dto.getDate());
        t.setClassId(dto.getClassId());
        t.setRoomId(dto.getRoomId());
        t.setClassName(dto.getClassName());
        t.setCourseName(dto.getCourseName());
        t.setRoomName(dto.getRoomName());
        t.setSessionIndex(dto.getSessionIndex());
        t.setTotalSessions(dto.getTotalSessions());
        t.setFixedSchedule(dto.getFixedSchedule());
        t.setStudents(dto.getStudents());
        return t;
    }

    private ScheduleItemForStudentDTO mapToStudentDTO(ScheduleItemDTO dto) {
        ScheduleItemForStudentDTO s = new ScheduleItemForStudentDTO();
        s.setId(dto.getId());
        s.setTitle(dto.getTitle());
        s.setStart(dto.getStart());
        s.setEnd(dto.getEnd());
        s.setDate(dto.getDate()); // new: propagate date
        s.setClassId(dto.getClassId());
        s.setTeacherId(dto.getTeacherId());
        s.setRoomId(dto.getRoomId());
        s.setClassName(dto.getClassName());
        s.setCourseName(dto.getCourseName());
        s.setTeacherName(dto.getTeacherName());
        s.setRoomName(dto.getRoomName());
        s.setSessionIndex(dto.getSessionIndex());
        s.setTotalSessions(dto.getTotalSessions());
        s.setFixedSchedule(dto.getFixedSchedule());
        return s;
    }

    // helper to create a stable key for deduplication
    private String makeKey(ScheduleItemDTO dto) {
        if (dto == null) return "null";
        if (dto.getId() != null && !dto.getId().isEmpty()) return dto.getId();
        // fallback composite key: classId|date|start|end|roomId
        return (dto.getClassId() != null ? dto.getClassId().toString() : "null")
                + "|" + (dto.getDate() != null ? dto.getDate() : (dto.getStart() != null ? dto.getStart() : ""))
                + "|" + (dto.getStart() != null ? dto.getStart() : "")
                + "|" + (dto.getEnd() != null ? dto.getEnd() : "")
                + "|" + (dto.getRoomId() != null ? dto.getRoomId().toString() : "null");
    }

}
