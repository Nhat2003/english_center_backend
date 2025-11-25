package com.example.English.Center.Data.repository.classes;

import com.example.English.Center.Data.entity.classes.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    // Find schedules for a given class
    List<Schedule> findByClassRoomId(Long classId);

    // Find schedules for a given teacher
    List<Schedule> findByTeacherId(Long teacherId);

    // Find schedules for classes that contain a given student
    List<Schedule> findByClassRoomStudentsId(Long studentId);

    void deleteByClassRoom_Id(Long classId);
}
