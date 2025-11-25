package com.example.English.Center.Data.repository.classes;

import com.example.English.Center.Data.entity.classes.ClassStudent;
import com.example.English.Center.Data.entity.classes.ClassRoom;
import com.example.English.Center.Data.entity.students.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassStudentRepository extends JpaRepository<ClassStudent, Long> {
    boolean existsByClassRoomAndStudent(ClassRoom classRoom, Student student);

    // Find all ClassStudent records for a given class id
    List<ClassStudent> findByClassRoom_Id(Long classId);

    void deleteByClassRoom_Id(Long classId);
}
