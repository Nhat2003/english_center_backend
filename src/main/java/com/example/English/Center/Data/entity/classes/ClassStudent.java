package com.example.English.Center.Data.entity.classes;

import jakarta.persistence.*;
import lombok.*;
import com.example.English.Center.Data.entity.students.Student;

@Entity
@Table(name = "class_students")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassStudent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private ClassRoom classRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
}
