package com.example.English.Center.Data.service.classes;

import com.example.English.Center.Data.entity.classes.ClassRoom;
import com.example.English.Center.Data.entity.classes.FixedSchedule;
import com.example.English.Center.Data.entity.classes.Room;
import com.example.English.Center.Data.entity.teachers.Teacher;
import com.example.English.Center.Data.entity.students.Student;
import com.example.English.Center.Data.repository.classes.ClassEntityRepository;
import com.example.English.Center.Data.util.DaysOfWeekUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Objects;

@Service
@Transactional
public class ClassRoomService {
    @Autowired
    private ClassEntityRepository classEntityRepository;

    public List<ClassRoom> getAll() {
        return classEntityRepository.findAll();
    }

    public Optional<ClassRoom> getById(Long id) {
        return classEntityRepository.findById(id);
    }

    public Optional<ClassRoom> getByName(String name) {
        return classEntityRepository.findByName(name);
    }

    public ClassRoom create(ClassRoom classRoom) {
        if (classEntityRepository.existsByName(classRoom.getName())) {
            throw new IllegalArgumentException("Tên lớp đã tồn tại!");
        }
        // ensure students are deduplicated before save
        if (classRoom.getStudents() != null && !classRoom.getStudents().isEmpty()) {
            classRoom.setStudents(new LinkedHashSet<>(classRoom.getStudents()));
        }

        // Kiểm tra conflict phòng: nếu có lớp khác dùng cùng phòng và trùng thời gian -> lỗi
        if (classRoom.getRoom() != null) {
            List<ClassRoom> candidates = classEntityRepository.findByRoom_Id(classRoom.getRoom().getId());
            for (ClassRoom other : candidates) {
                if (other == null) continue;
                if (other.getId() != null && Objects.equals(other.getId(), classRoom.getId())) continue;
                if (other.getRoom() != null && Objects.equals(other.getRoom().getId(), classRoom.getRoom().getId())) {
                    if (isScheduleConflict(classRoom, other)) {
                        throw new IllegalArgumentException("Xung đột phòng: Phòng '" + classRoom.getRoom().getName() + "' đã được sử dụng bởi lớp '" + other.getName() + "' (id=" + other.getId() + ") vào thời gian trùng.");
                    }
                }
            }
        }

        // Kiểm tra conflict cho từng học sinh đã được thêm vào lớp mới
        if (classRoom.getStudents() != null) {
            for (Student s : classRoom.getStudents()) {
                if (s == null || s.getId() == null) continue;
                List<ClassRoom> existing = classEntityRepository.findByStudents_Id(s.getId());
                for (ClassRoom other : existing) {
                    if (other == null) continue;
                    if (other.getId() != null && Objects.equals(other.getId(), classRoom.getId())) continue;
                    if (isScheduleConflict(classRoom, other)) {
                        throw new IllegalArgumentException("Xung đột lịch: Học sinh '" + (s.getFullName() != null ? s.getFullName() : "id=" + s.getId()) + "' (id=" + s.getId() + ") đã có lớp '" + other.getName() + "' (id=" + other.getId() + ") vào thời gian trùng.");
                    }
                }
            }
        }

        // Save the class only. Schedule rows will be generated on-the-fly when requested.
        ClassRoom savedClass = classEntityRepository.save(classRoom);
        return savedClass;
    }

    public ClassRoom update(Long id, ClassRoom classRoom) {
        classRoom.setId(id);
        // deduplicate students before save
        if (classRoom.getStudents() != null && !classRoom.getStudents().isEmpty()) {
            classRoom.setStudents(new LinkedHashSet<>(classRoom.getStudents()));
        }

        // Kiểm tra phòng conflict khi update
        if (classRoom.getRoom() != null) {
            List<ClassRoom> candidates = classEntityRepository.findByRoom_Id(classRoom.getRoom().getId());
            for (ClassRoom other : candidates) {
                if (other == null) continue;
                if (other.getId() != null && Objects.equals(other.getId(), classRoom.getId())) continue;
                if (other.getRoom() != null && Objects.equals(other.getRoom().getId(), classRoom.getRoom().getId())) {
                    if (isScheduleConflict(classRoom, other)) {
                        throw new IllegalArgumentException("Xung đột phòng: Phòng '" + classRoom.getRoom().getName() + "' đã được sử dụng bởi lớp '" + other.getName() + "' (id=" + other.getId() + ") vào thời gian trùng.");
                    }
                }
            }
        }

        // Kiểm tra conflict cho từng học sinh
        if (classRoom.getStudents() != null) {
            for (Student s : classRoom.getStudents()) {
                if (s == null || s.getId() == null) continue;
                List<ClassRoom> existing = classEntityRepository.findByStudents_Id(s.getId());
                for (ClassRoom other : existing) {
                    if (other == null) continue;
                    if (other.getId() != null && Objects.equals(other.getId(), classRoom.getId())) continue;
                    if (isScheduleConflict(classRoom, other)) {
                        throw new IllegalArgumentException("Xung đột lịch: Học sinh '" + (s.getFullName() != null ? s.getFullName() : "id=" + s.getId()) + "' (id=" + s.getId() + ") đã có lớp '" + other.getName() + "' (id=" + other.getId() + ") vào thời gian trùng.");
                    }
                }
            }
        }

        return classEntityRepository.save(classRoom);
    }

    public void delete(Long id) {
        classEntityRepository.deleteById(id);
    }

    // New: get classes for a given student id
    public List<ClassRoom> getByStudentId(Long studentId) {
        return classEntityRepository.findByStudents_Id(studentId);
    }

    // New: get classes taught by a given teacher id
    public List<ClassRoom> getByTeacherId(Long teacherId) {
        return classEntityRepository.findByTeacher_Id(teacherId);
    }

    public LocalDate calculateEndDate(LocalDate startDate, String daysOfWeek, int duration) {
        Set<Integer> days = DaysOfWeekUtil.vnStringToJavaDows(daysOfWeek);
        LocalDate date = startDate;
        int count = 0;
        while (count < duration) {
            int dayOfWeek = date.getDayOfWeek().getValue(); // 1=Monday..7=Sunday
            if (days.contains(dayOfWeek)) {
                count++;
            }
            if (count < duration) {
                date = date.plusDays(1);
            }
        }
        return date;
    }

    // --- Helpers for schedule conflict detection (similar logic to ClassStudentService) ---
    private boolean isScheduleConflict(ClassRoom a, ClassRoom b) {
        if (a == null || b == null) return false;

        if (a.getFixedSchedule() == null || b.getFixedSchedule() == null) return false;

        // Kiểm tra khoảng ngày overlap
        LocalDate start = a.getStartDate().isAfter(b.getStartDate()) ? a.getStartDate() : b.getStartDate();
        LocalDate end = a.getEndDate().isBefore(b.getEndDate()) ? a.getEndDate() : b.getEndDate();
        if (start.isAfter(end)) return false;

        Set<Integer> daysA = parseDaysOfWeek(a.getFixedSchedule().getDaysOfWeek());
        Set<Integer> daysB = parseDaysOfWeek(b.getFixedSchedule().getDaysOfWeek());
        Set<Integer> intersection = new HashSet<>(daysA);
        intersection.retainAll(daysB);
        if (intersection.isEmpty()) return false;

        LocalTime aStart = parseTimeSafe(a.getFixedSchedule().getStartTime());
        LocalTime aEnd = parseTimeSafe(a.getFixedSchedule().getEndTime());
        LocalTime bStart = parseTimeSafe(b.getFixedSchedule().getStartTime());
        LocalTime bEnd = parseTimeSafe(b.getFixedSchedule().getEndTime());
        if (aStart == null || aEnd == null || bStart == null || bEnd == null) return false;

        boolean timeOverlap = aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
        if (!timeOverlap) return false;

        if (!hasDateWithDayInRange(start, end, intersection)) return false;

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
