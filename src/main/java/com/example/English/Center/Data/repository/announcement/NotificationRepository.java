package com.example.English.Center.Data.repository.announcement;

import com.example.English.Center.Data.entity.announcement.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByStudent_IdOrderByCreatedAtDesc(Long studentId);
    List<Notification> findByClassRoom_IdAndStudent_IdOrderByCreatedAtDesc(Long classId, Long studentId);

    long countByStudent_IdAndReadFlagFalse(Long studentId);

    void deleteByClassRoom_Id(Long classId);
}
