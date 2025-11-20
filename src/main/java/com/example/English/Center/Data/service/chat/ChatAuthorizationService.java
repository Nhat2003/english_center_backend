package com.example.English.Center.Data.service.chat;

import com.example.English.Center.Data.repository.classes.ClassEntityRepository;
import com.example.English.Center.Data.repository.students.StudentRepository;
import com.example.English.Center.Data.repository.teachers.TeacherRepository;
import com.example.English.Center.Data.repository.users.UserRepository;
import com.example.English.Center.Data.entity.users.UserRole;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ChatAuthorizationService {
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ClassEntityRepository classEntityRepository;
    private final UserRepository userRepository;

    public ChatAuthorizationService(StudentRepository studentRepository,
                                    TeacherRepository teacherRepository,
                                    ClassEntityRepository classEntityRepository,
                                    UserRepository userRepository) {
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.classEntityRepository = classEntityRepository;
        this.userRepository = userRepository;
    }

    // Return set of class ids that the user (identified by userId) participates in (as student or teacher)
    public Set<Long> getClassIdsForUser(Long userId) {
        Set<Long> set = new HashSet<>();
        if (userId == null) return set;

        // as student
        studentRepository.findByUserId(userId).ifPresent(student -> {
            List<com.example.English.Center.Data.entity.classes.ClassRoom> classes = classEntityRepository.findByStudents_Id(student.getId());
            for (com.example.English.Center.Data.entity.classes.ClassRoom c : classes) {
                if (c != null && c.getId() != null) set.add(c.getId());
            }
        });

        // as teacher
        teacherRepository.findByUserId(userId).ifPresent(teacher -> {
            List<com.example.English.Center.Data.entity.classes.ClassRoom> classes = classEntityRepository.findByTeacher_Id(teacher.getId());
            for (com.example.English.Center.Data.entity.classes.ClassRoom c : classes) {
                if (c != null && c.getId() != null) set.add(c.getId());
            }
        });

        return set;
    }

    public boolean inSameClass(Long userAId, Long userBId) {
        if (userAId == null || userBId == null) return false;
        Set<Long> a = getClassIdsForUser(userAId);
        if (a.isEmpty()) return false;
        Set<Long> b = getClassIdsForUser(userBId);
        if (b.isEmpty()) return false;
        for (Long id : a) if (b.contains(id)) return true;
        return false;
    }

    // allow messaging if same class OR either user is ADMIN
    public boolean canMessage(Long userAId, Long userBId) {
        if (userAId == null || userBId == null) return false;
        if (userAId.equals(userBId)) return true; // self message (harmless)

        // Check admin bypass
        var aRole = userRepository.findById(userAId).map(u -> u.getRole()).orElse(null);
        var bRole = userRepository.findById(userBId).map(u -> u.getRole()).orElse(null);
        if (aRole == UserRole.ADMIN || bRole == UserRole.ADMIN) return true;

        // fallback to same class rule
        return inSameClass(userAId, userBId);
    }
}
