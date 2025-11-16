package com.example.English.Center.Data.entity.assignment;

import com.example.English.Center.Data.entity.classes.ClassRoom;
import com.example.English.Center.Data.entity.submission.Submission;
import com.example.English.Center.Data.entity.teachers.Teacher;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;



@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "assignment")
public class Assignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;
    private String fileUrl;

    private LocalDate dueDate;

    @ManyToOne
    @JoinColumn(name = "classRoom_id", nullable = false)
    private ClassRoom classRoom;

    @ManyToOne
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Submission> submissions;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Updated the field name to match the new convention
    public Long getClassRoomId() {
        return classRoom != null ? classRoom.getId() : null;
    }

    public void setClassRoomId(Long classRoomId) {
        if (this.classRoom == null) {
            this.classRoom = new ClassRoom();
        }
        this.classRoom.setId(classRoomId);
    }
}
