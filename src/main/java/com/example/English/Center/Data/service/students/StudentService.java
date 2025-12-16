package com.example.English.Center.Data.service.students;

import com.example.English.Center.Data.dto.students.StudentRequest;
import com.example.English.Center.Data.dto.students.StudentResponse;
import com.example.English.Center.Data.dto.profile.UpdateProfileRequest;
import com.example.English.Center.Data.entity.students.Student;
import com.example.English.Center.Data.entity.users.User;
import com.example.English.Center.Data.repository.students.StudentRepository;
import com.example.English.Center.Data.repository.users.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    // New: trả về Page để controller có thể trả pagination metadata
    public Page<StudentResponse> getAllStudentsPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return studentRepository.findAll(pageable).map(StudentResponse::new);
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
        // className sẽ tự động được cập nhật khi thêm học sinh vào lớp
        student.setClassName(null);
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
        // Không cập nhật className từ request - sẽ tự động cập nhật qua class membership
        // student.setClassName(request.getClassName());
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

    // Student updates their own profile
    public StudentResponse updateMyProfile(Long studentId, UpdateProfileRequest request) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student not found with id " + studentId));

        if (request.getFullName() != null) {
            student.setFullName(request.getFullName());
        }
        if (request.getDob() != null) {
            student.setDob(request.getDob());
        }
        if (request.getGender() != null) {
            student.setGender(request.getGender());
        }
        if (request.getPhone() != null) {
            student.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            student.setAddress(request.getAddress());
        }
        if (request.getEmail() != null) {
            student.setEmail(request.getEmail());
        }

        Student updated = studentRepository.save(student);
        return new StudentResponse(updated);
    }

    // Search students by q (name/email/className). Returns a Page of StudentResponse for pagination
    public org.springframework.data.domain.Page<StudentResponse> searchStudents(String q, int page, int size) {
        if (q == null || q.trim().isEmpty()) {
            return getAllStudentsPage(page, size);
        }
        String key = q.trim();
        List<Student> byName = studentRepository.findByFullNameContainingIgnoreCase(key);
        List<Student> byEmail = studentRepository.findByEmailContainingIgnoreCase(key);
        List<Student> byClass = studentRepository.findByClassNameContainingIgnoreCase(key);
        // merge unique by id preserving order
        Map<Long, Student> map = new java.util.LinkedHashMap<>();
        if (byName != null) byName.forEach(s -> map.put(s.getId(), s));
        if (byEmail != null) byEmail.forEach(s -> map.put(s.getId(), s));
        if (byClass != null) byClass.forEach(s -> map.put(s.getId(), s));
        List<StudentResponse> all = new ArrayList<>();
        for (Student s : map.values()) all.add(new StudentResponse(s));
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        List<StudentResponse> content = all.subList(from, to);
        return new PageImpl<>(content, PageRequest.of(page, size), all.size());
    }

    // Return full search result (no pagination) as List<StudentResponse> for controller-side filtering/paging
    public List<StudentResponse> searchStudentsRaw(String q) {
        if (q == null || q.trim().isEmpty()) return studentRepository.findAll().stream().map(StudentResponse::new).collect(Collectors.toList());
        String key = q.trim();
        List<Student> byName = studentRepository.findByFullNameContainingIgnoreCase(key);
        List<Student> byEmail = studentRepository.findByEmailContainingIgnoreCase(key);
        List<Student> byClass = studentRepository.findByClassNameContainingIgnoreCase(key);
        Map<Long, Student> map = new java.util.LinkedHashMap<>();
        if (byName != null) byName.forEach(s -> map.put(s.getId(), s));
        if (byEmail != null) byEmail.forEach(s -> map.put(s.getId(), s));
        if (byClass != null) byClass.forEach(s -> map.put(s.getId(), s));
        List<StudentResponse> all = new ArrayList<>();
        for (Student s : map.values()) all.add(new StudentResponse(s));
        return all;
    }
}
