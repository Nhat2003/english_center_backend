package com.example.English.Center.Data.repository.attendance;

import com.example.English.Center.Data.entity.attendance.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByClassRoomIdAndSessionDate(Long classId, LocalDate sessionDate);
    List<Attendance> findByStudentId(Long studentId);

    // Check if a session exists for a class on a specific date
    boolean existsByClassRoomIdAndSessionDate(Long classId, LocalDate sessionDate);

    // Count how many distinct session dates have been recorded for a class
    @Query("select count(distinct a.sessionDate) from Attendance a where a.classRoom.id = :classId")
    long countDistinctSessionDatesByClassId(@Param("classId") Long classId);

    // Get list of distinct session dates for a class (optional use)
    @Query("select distinct a.sessionDate from Attendance a where a.classRoom.id = :classId order by a.sessionDate")
    List<LocalDate> findDistinctSessionDatesByClassId(@Param("classId") Long classId);
}
