package com.example.English.Center.Data.service.classes;

import com.example.English.Center.Data.entity.classes.ClassRoom;
import com.example.English.Center.Data.entity.classes.FixedSchedule;
import com.example.English.Center.Data.entity.classes.Room;
import com.example.English.Center.Data.entity.teachers.Teacher;
import com.example.English.Center.Data.entity.students.Student;
import com.example.English.Center.Data.repository.classes.ClassEntityRepository;
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
        // Save the class only. Schedule rows will be generated on-the-fly when requested.
        ClassRoom savedClass = classEntityRepository.save(classRoom);
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
