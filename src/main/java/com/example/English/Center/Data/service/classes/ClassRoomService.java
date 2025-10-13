package com.example.English.Center.Data.service.classes;

import com.example.English.Center.Data.entity.classes.ClassRoom;
import com.example.English.Center.Data.entity.classes.FixedSchedule;
import com.example.English.Center.Data.entity.classes.Schedule;
import com.example.English.Center.Data.entity.classes.Room;
import com.example.English.Center.Data.entity.teachers.Teacher;
import com.example.English.Center.Data.entity.students.Student;
import com.example.English.Center.Data.repository.classes.ClassEntityRepository;
import com.example.English.Center.Data.repository.classes.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Service
public class ClassRoomService {
    @Autowired
    private ClassEntityRepository classEntityRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

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
        ClassRoom savedClass = classEntityRepository.save(classRoom);
        FixedSchedule fixedSchedule = savedClass.getFixedSchedule();
        Teacher teacher = savedClass.getTeacher();
        Room room = savedClass.getRoom();
        List<Student> students = savedClass.getStudents();
        // Giả sử startDate và duration được truyền vào từ classRoom
        LocalDate start = savedClass.getStartDate();
        int duration = savedClass.getCourse() != null ? savedClass.getCourse().getDuration() : 0; // số buổi học
        if (fixedSchedule != null && teacher != null && room != null && start != null && duration > 0) {
            String daysOfWeek = fixedSchedule.getDaysOfWeek(); // ví dụ: "2,4,6"
            LocalDate end = calculateEndDate(start, daysOfWeek, duration);
            java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");
            java.time.ZoneId zoneId = java.time.ZoneId.of("Asia/Ho_Chi_Minh");
            java.time.LocalTime st = java.time.LocalTime.parse(fixedSchedule.getStartTime(), timeFormatter);
            java.time.LocalTime et = java.time.LocalTime.parse(fixedSchedule.getEndTime(), timeFormatter);
            Set<Integer> days = new HashSet<>();
            for (String d : daysOfWeek.split(",")) {
                days.add(Integer.parseInt(d));
            }
            int session = 1;
            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                int dayOfWeek = date.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday
                if (days.contains(dayOfWeek)) {
                    java.time.ZonedDateTime startDateTime = java.time.ZonedDateTime.of(date, st, zoneId);
                    java.time.ZonedDateTime endDateTime = java.time.ZonedDateTime.of(date, et, zoneId);
                    String title = "Buổi " + session + " - Lớp " + savedClass.getName();
                    Schedule schedule = Schedule.builder()
                        .name("class-" + savedClass.getId() + "-" + date)
                        .title(title)
                        .classRoom(savedClass)
                        .teacher(teacher)
                        .room(room)
                        .startDateTime(startDateTime)
                        .endDateTime(endDateTime)
                        .description("")
                        .build();
                    scheduleRepository.save(schedule);
                    session++;
                    if (session > duration) break;
                }
            }
        }
        return savedClass;
    }

    public ClassRoom update(Long id, ClassRoom classRoom) {
        classRoom.setId(id);
        return classEntityRepository.save(classRoom);
    }

    public void delete(Long id) {
        classEntityRepository.deleteById(id);
    }

    public LocalDate calculateEndDate(LocalDate startDate, String daysOfWeek, int duration) {
        Set<Integer> days = new HashSet<>();
        for (String d : daysOfWeek.split(",")) {
            days.add(Integer.parseInt(d));
        }
        LocalDate date = startDate;
        int count = 0;
        while (count < duration) {
            int dayOfWeek = date.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday
            if (days.contains(dayOfWeek)) {
                count++;
            }
            if (count < duration) {
                date = date.plusDays(1);
            }
        }
        return date;
    }
}
