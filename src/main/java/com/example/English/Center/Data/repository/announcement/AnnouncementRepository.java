package com.example.English.Center.Data.repository.announcement;

import com.example.English.Center.Data.entity.announcement.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findByClassRoom_IdOrderByCreatedAtDesc(Long classId);
    void deleteByClassRoom_Id(Long classId);
}

