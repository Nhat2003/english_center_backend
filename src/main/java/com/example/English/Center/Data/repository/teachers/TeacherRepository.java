package com.example.English.Center.Data.repository.teachers;

import com.example.English.Center.Data.entity.teachers.Teacher;
import com.example.English.Center.Data.entity.users.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    void deleteByUserId(Long userId);
    Optional<Teacher> findByUserId(Long userId);
}
