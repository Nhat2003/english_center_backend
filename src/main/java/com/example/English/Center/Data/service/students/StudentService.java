package com.example.English.Center.Data.service.students;

import com.example.English.Center.Data.dto.students.StudentRequest;
import com.example.English.Center.Data.dto.students.StudentResponse;
import com.example.English.Center.Data.entity.students.Student;
import com.example.English.Center.Data.entity.users.User;
import com.example.English.Center.Data.repository.students.StudentRepository;
import com.example.English.Center.Data.repository.users.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StudentService {
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public StudentService(StudentRepository studentRepository, UserRepository userRepository) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
    }

    // Lấy danh sách tất cả học viên (có phân trang)
    public List<StudentResponse> getAllStudents(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return studentRepository.findAll(pageable)
                .getContent()
                .stream()
                .map(StudentResponse::new)   // map entity -> response
                .collect(Collectors.toList());
    }

    // Lấy học viên theo id
    public StudentResponse getStudentById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Student not found with id " + id));
        return new StudentResponse(student);
    }

    // Tạo học viên mới
    public StudentResponse createStudent(StudentRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getRole() != com.example.English.Center.Data.entity.users.UserRole.STUDENT) {
            throw new RuntimeException("User must have role STUDENT");
        }
        if (studentRepository.findByUserId(request.getUserId()).isPresent()) {
            throw new RuntimeException("UserId này đã được liên kết với một học sinh khác");
        }
        Student student = new Student();
        student.setUser(user);
        student.setFullName(request.getFullName());
        student.setClassName(request.getClassName());
        student.setEmail(request.getEmail());
        student.setDob(request.getDob());
        student.setGender(request.getGender());
        student.setPhone(request.getPhone());
        student.setAddress(request.getAddress());
        student.setJoinedAt(request.getJoinedAt());

        Student savedStudent = studentRepository.save(student);
        return new StudentResponse(savedStudent);
    }

    // Cập nhật học viên
    public StudentResponse updateStudent(Long id, StudentRequest request) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Student not found with id " + id));

        if (request.getUserId() != null) {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new EntityNotFoundException("User not found with id " + request.getUserId()));
            student.setUser(user);
        }

        student.setFullName(request.getFullName());
        student.setClassName(request.getClassName());
        student.setEmail(request.getEmail());
        student.setDob(request.getDob());
        student.setGender(request.getGender());
        student.setPhone(request.getPhone());
        student.setAddress(request.getAddress());
        student.setJoinedAt(request.getJoinedAt());

        Student updated = studentRepository.save(student);
        return new StudentResponse(updated);
    }

    // Xoá học viên
    public void deleteStudent(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new EntityNotFoundException("Student not found with id " + id);
        }
        studentRepository.deleteById(id);
    }
}
