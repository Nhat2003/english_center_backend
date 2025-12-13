package com.example.English.Center.Data.entity.classes;

import com.example.English.Center.Data.entity.users.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "session_change_audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionChangeAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "session_override_id")
    private SessionOverride sessionOverride;

    @ManyToOne
    @JoinColumn(name = "class_room_id")
    private ClassRoom classRoom;

    @Enumerated(EnumType.STRING)
    private Action action;

    @ManyToOne
    @JoinColumn(name = "changed_by")
    private User changedBy;

    private LocalDateTime changedAt;

    @Column(columnDefinition = "TEXT")
    private String details;

    public enum Action { CREATED, UPDATED, REVERTED, CANCELLED }
}

