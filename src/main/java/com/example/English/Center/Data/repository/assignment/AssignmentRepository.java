package com.example.English.Center.Data.repository.assignment;

import com.example.English.Center.Data.entity.assignment.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    @Query("SELECT a FROM Assignment a WHERE a.classRoom.id = :classRoomId")
    List<Assignment> findByClassRoomId(Long classRoomId);

    List<Assignment> findByClassRoom_IdIn(Collection<Long> classIds);

    void deleteByClassRoom_Id(Long classId);
}
