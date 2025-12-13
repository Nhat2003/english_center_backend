package com.example.English.Center.Data.entity.classes;

import com.example.English.Center.Data.entity.users.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "session_override", uniqueConstraints = {@UniqueConstraint(columnNames = {"class_room_id", "original_date"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionOverride {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "class_room_id", nullable = false)
    private ClassRoom classRoom;

    @Column(name = "original_date", nullable = false)
    private LocalDate originalDate; // the date of the occurrence being moved

    private LocalDateTime originalStart;
    private LocalDateTime originalEnd;

    private LocalDateTime newStart;
    private LocalDateTime newEnd;

    @Enumerated(EnumType.STRING)
    private OverrideStatus status;

    private String reason;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum OverrideStatus { ACTIVE, CANCELLED }
}
