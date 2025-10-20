package com.example.English.Center.Data.repository.classes;

import com.example.English.Center.Data.entity.classes.ClassRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface ClassEntityRepository extends JpaRepository<ClassRoom, Long> {
    Optional<ClassRoom> findByName(String name);
    boolean existsByName(String name);

    // Find classes that contain a given student
    List<ClassRoom> findByStudents_Id(Long studentId);
}
