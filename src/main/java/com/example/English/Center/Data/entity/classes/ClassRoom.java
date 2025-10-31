package com.example.English.Center.Data.entity.classes;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = "students")
@ToString(exclude = "students")
public class ClassRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private com.example.English.Center.Data.entity.courses.Course course;

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private com.example.English.Center.Data.entity.teachers.Teacher teacher;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private com.example.English.Center.Data.entity.classes.Room room;

    @ManyToOne
    @JoinColumn(name = "fixed_schedule_id")
    private com.example.English.Center.Data.entity.classes.FixedSchedule fixedSchedule;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "class_students",
        joinColumns = @JoinColumn(name = "class_id"),
        inverseJoinColumns = @JoinColumn(name = "student_id"),
        uniqueConstraints = @UniqueConstraint(columnNames = {"class_id", "student_id"})
    )
    private Set<com.example.English.Center.Data.entity.students.Student> students;
}
