package com.example.English.Center.Data.repository.classes;

import com.example.English.Center.Data.entity.classes.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByStudentId(Long studentId);
}
