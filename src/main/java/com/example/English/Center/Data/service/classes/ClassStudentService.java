package com.example.English.Center.Data.service.classes;

import com.example.English.Center.Data.entity.classes.ClassRoom;
import com.example.English.Center.Data.entity.classes.ClassStudent;
import com.example.English.Center.Data.entity.students.Student;
import com.example.English.Center.Data.repository.classes.ClassStudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ClassStudentService {
    @Autowired
    private ClassStudentRepository classStudentRepository;

    public List<ClassStudent> getAll() {
        return classStudentRepository.findAll();
    }

    public Optional<ClassStudent> getById(Long id) {
        return classStudentRepository.findById(id);
    }

    public ClassStudent create(ClassRoom classRoom, Student student) {
        if (classStudentRepository.existsByClassRoomAndStudent(classRoom, student)) {
            throw new IllegalArgumentException("Học sinh đã đăng ký lớp này!");
        }
        ClassStudent classStudent = ClassStudent.builder()
                .classRoom(classRoom)
                .student(student)
                .build();
        return classStudentRepository.save(classStudent);
    }

    public void delete(Long id) {
        classStudentRepository.deleteById(id);
    }
}
