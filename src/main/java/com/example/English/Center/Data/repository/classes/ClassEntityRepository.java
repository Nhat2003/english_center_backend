package com.example.English.Center.Data.repository.classes;

import com.example.English.Center.Data.entity.classes.ClassRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ClassEntityRepository extends JpaRepository<ClassRoom, Long> {
    Optional<ClassRoom> findByName(String name);
    boolean existsByName(String name);
}

