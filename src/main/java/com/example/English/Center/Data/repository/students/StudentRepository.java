package com.example.English.Center.Data.repository.students;


import com.example.English.Center.Data.entity.students.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    void deleteByUserId(Long userId);
    Optional<Student> findByUserId(Long userId);
    Optional<Student> findByEmail(String email);
    Optional<Student> findByEmailIgnoreCase(String email);
    // search helpers
    List<Student> findByFullNameContainingIgnoreCase(String key);
    List<Student> findByEmailContainingIgnoreCase(String key);
    List<Student> findByClassNameContainingIgnoreCase(String key);
}
