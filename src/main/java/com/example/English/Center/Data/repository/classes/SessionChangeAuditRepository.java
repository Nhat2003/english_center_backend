package com.example.English.Center.Data.repository.classes;

import com.example.English.Center.Data.entity.classes.SessionChangeAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionChangeAuditRepository extends JpaRepository<SessionChangeAudit, Long> {
    List<SessionChangeAudit> findByClassRoom_IdOrderByChangedAtDesc(Long classRoomId);
}

