package com.example.English.Center.Data.repository.students;


import com.example.English.Center.Data.entity.students.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {
}

