package com.example.English.Center.Data.service.classes;

import com.example.English.Center.Data.entity.classes.ClassRoom;
import com.example.English.Center.Data.entity.classes.ClassStudent;
import com.example.English.Center.Data.entity.students.Student;
import com.example.English.Center.Data.repository.classes.ClassStudentRepository;
import com.example.English.Center.Data.repository.classes.ClassEntityRepository;
import com.example.English.Center.Data.dto.students.StudentResponse;
import com.example.English.Center.Data.util.DaysOfWeekUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClassStudentService {
    @Autowired
    private ClassStudentRepository classStudentRepository;

    @Autowired
    private ClassEntityRepository classEntityRepository; // để lấy các lớp của học sinh

    public List<ClassStudent> getAll() {
        return classStudentRepository.findAll();
    }

    public Optional<ClassStudent> getById(Long id) {
        return classStudentRepository.findById(id);
    }

    public ClassStudent create(ClassRoom classRoom, Student student) {
        if (classStudentRepository.existsByClassRoomAndStudent(classRoom, student)) {
            throw new IllegalArgumentException("Học sinh đã đăng ký lớp này!");
        }

        // Lấy tất cả lớp mà học sinh đã tham gia
        List<ClassRoom> existingClasses = classEntityRepository.findByStudents_Id(student.getId());
        for (ClassRoom other : existingClasses) {
            if (other == null) continue;
            if (isScheduleConflict(classRoom, other)) {
                throw new IllegalArgumentException("Xung đột lịch: Học sinh đã có buổi học trùng lịch hoặc trùng phòng vào cùng thời gian.");
            }
        }

        ClassStudent classStudent = ClassStudent.builder()
                .classRoom(classRoom)
                .student(student)
                .build();
        return classStudentRepository.save(classStudent);
    }

    public void delete(Long id) {
        classStudentRepository.deleteById(id);
    }

    // New: get students for a given class id as StudentResponse DTOs
    public List<StudentResponse> getStudentsByClassId(Long classId) {
        List<ClassStudent> classStudents = classStudentRepository.findByClassRoom_Id(classId);
        return classStudents.stream()
                .map(cs -> new StudentResponse(cs.getStudent()))
                .collect(Collectors.toList());
    }

    // Kiểm tra xung đột giữa 2 lớp
    private boolean isScheduleConflict(ClassRoom a, ClassRoom b) {
        if (a == null || b == null) return false;

        // Nếu không có fixed schedule thì không thể so sánh chính xác -> coi là không conflict
        if (a.getFixedSchedule() == null || b.getFixedSchedule() == null) return false;

        // Kiểm tra khoảng thời gian lớp (date range) có overlap hay không
        LocalDate start = a.getStartDate().isAfter(b.getStartDate()) ? a.getStartDate() : b.getStartDate();
        LocalDate end = a.getEndDate().isBefore(b.getEndDate()) ? a.getEndDate() : b.getEndDate();
        if (start.isAfter(end)) return false; // không có ngày chung

        // Parse days of week
        Set<Integer> daysA = parseDaysOfWeek(a.getFixedSchedule().getDaysOfWeek());
        Set<Integer> daysB = parseDaysOfWeek(b.getFixedSchedule().getDaysOfWeek());
        Set<Integer> intersection = new HashSet<>(daysA);
        intersection.retainAll(daysB);
        if (intersection.isEmpty()) return false; // không có ngày trùng

        // Parse times
        LocalTime aStart = parseTimeSafe(a.getFixedSchedule().getStartTime());
        LocalTime aEnd = parseTimeSafe(a.getFixedSchedule().getEndTime());
        LocalTime bStart = parseTimeSafe(b.getFixedSchedule().getStartTime());
        LocalTime bEnd = parseTimeSafe(b.getFixedSchedule().getEndTime());
        if (aStart == null || aEnd == null || bStart == null || bEnd == null) return false;

        boolean timeOverlap = aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
        if (!timeOverlap) return false;

        // Nếu thời gian overlap và có ngày trong tuần trùng thì cần kiểm tra xem có ít nhất 1 ngày thực tế trong khoảng date range trùng ngày trong tuần
        if (!hasDateWithDayInRange(start, end, intersection)) return false;

        // Nếu đến đây nghĩa là có cùng thời điểm vào cùng ngày -> conflict
        return true;
    }

    private LocalTime parseTimeSafe(String t) {
        try {
            if (t == null) return null;
            return LocalTime.parse(t);
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean hasDateWithDayInRange(LocalDate start, LocalDate end, Set<Integer> days) {
        LocalDate cur = start;
        while (!cur.isAfter(end)) {
            int dow = cur.getDayOfWeek().getValue(); // 1..7
            if (days.contains(dow)) return true;
            cur = cur.plusDays(1);
        }
        return false;
    }

    private Set<Integer> parseDaysOfWeek(String daysOfWeek) {
        return DaysOfWeekUtil.vnStringToJavaDows(daysOfWeek);
    }
}
