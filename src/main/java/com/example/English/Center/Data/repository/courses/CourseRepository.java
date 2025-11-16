package com.example.English.Center.Data.repository.courses;

import com.example.English.Center.Data.entity.courses.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
}

