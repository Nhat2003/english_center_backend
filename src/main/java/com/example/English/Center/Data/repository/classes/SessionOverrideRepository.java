package com.example.English.Center.Data.repository.classes;

import com.example.English.Center.Data.entity.classes.SessionOverride;
import com.example.English.Center.Data.entity.classes.SessionOverride.OverrideStatus;
import com.example.English.Center.Data.entity.classes.ClassRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

public interface SessionOverrideRepository extends JpaRepository<SessionOverride, Long> {
    Optional<SessionOverride> findByClassRoomAndOriginalDateAndStatus(ClassRoom classRoom, LocalDate originalDate, OverrideStatus status);
    List<SessionOverride> findByClassRoomIdAndStatus(Long classRoomId, OverrideStatus status);
}

