package com.example.English.Center.Data.service.attendance;

import com.example.English.Center.Data.entity.attendance.Attendance;
import com.example.English.Center.Data.repository.attendance.AttendanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;

    public AttendanceService(AttendanceRepository attendanceRepository) {
        this.attendanceRepository = attendanceRepository;
    }

    public List<Attendance> getAll() {
        return attendanceRepository.findAll();
    }

    public Optional<Attendance> getById(Long id) {
        return attendanceRepository.findById(id);
    }

    public List<Attendance> getByClassAndDate(Long classId, LocalDate date) {
        return attendanceRepository.findByClassRoomIdAndSessionDate(classId, date);
    }

    public List<Attendance> getByStudent(Long studentId) {
        return attendanceRepository.findByStudentId(studentId);
    }

    public Attendance save(Attendance attendance) {
        return attendanceRepository.save(attendance);
    }

    public void delete(Long id) {
        attendanceRepository.deleteById(id);
    }

    // Save bulk attendance records for a class session
    @Transactional
    public List<Attendance> saveSession(List<Attendance> attendances) {
        return attendanceRepository.saveAll(attendances);
    }

    // Replace a whole session: delete existing records for class/date then save provided list
    @Transactional
    public List<Attendance> replaceSession(Long classId, LocalDate date, List<Attendance> attendances) {
        List<Attendance> existing = attendanceRepository.findByClassRoomIdAndSessionDate(classId, date);
        if (!existing.isEmpty()) {
            attendanceRepository.deleteAll(existing);
        }
        return attendanceRepository.saveAll(attendances);
    }

    public boolean sessionExists(Long classId, LocalDate date) {
        return attendanceRepository.existsByClassRoomIdAndSessionDate(classId, date);
    }

    public long countDistinctSessions(Long classId) {
        return attendanceRepository.countDistinctSessionDatesByClassId(classId);
    }
}
