package com.example.English.Center.Data.entity.submission;


import com.example.English.Center.Data.entity.assignment.Assignment;
import com.example.English.Center.Data.entity.students.Student;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "assignment_id", nullable = false)
    @JsonBackReference
    private Assignment assignment;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    private LocalDateTime submittedAt;

    private String fileUrl;

    private Double grade;

    @Column(columnDefinition = "TEXT")
    private String feedback;
}
