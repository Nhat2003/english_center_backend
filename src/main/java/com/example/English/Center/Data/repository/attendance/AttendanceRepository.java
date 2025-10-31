package com.example.English.Center.Data.repository.attendance;

import com.example.English.Center.Data.entity.attendance.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByClassRoomIdAndSessionDate(Long classId, LocalDate sessionDate);
    List<Attendance> findByStudentId(Long studentId);
}
