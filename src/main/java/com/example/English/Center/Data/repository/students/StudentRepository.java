package com.example.English.Center.Data.repository.students;


import com.example.English.Center.Data.entity.students.Student;
import com.example.English.Center.Data.entity.users.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    void deleteByUserId(Long userId);
    Optional<Student> findByUserId(Long userId);
}
