package com.example.English.Center.Data.service.users;

import com.example.English.Center.Data.dto.students.StudentResponse;
import com.example.English.Center.Data.dto.teachers.TeacherResponse;
import com.example.English.Center.Data.dto.users.UserRequest;
import com.example.English.Center.Data.dto.users.UserResponse;
import com.example.English.Center.Data.entity.students.Student;
import com.example.English.Center.Data.entity.teachers.Teacher;
import com.example.English.Center.Data.entity.users.User;
import com.example.English.Center.Data.entity.users.UserRole;
import com.example.English.Center.Data.repository.students.StudentRepository;
import com.example.English.Center.Data.repository.teachers.TeacherRepository;
import com.example.English.Center.Data.repository.users.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;

    public UserService(UserRepository userRepository, TeacherRepository teacherRepository, StudentRepository studentRepository) {
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User createUser(User user) {
        user.setIsActive(true);
        return userRepository.save(user);
    }

    public User updateUser(Long id, User updatedUser) {
        return userRepository.findById(id).map(user -> {
            user.setUsername(updatedUser.getUsername());
            user.setRole(updatedUser.getRole());
            user.setIsActive(updatedUser.getIsActive());
            // Chỉ cập nhật password nếu không null và không rỗng
            if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
                user.setPassword(updatedUser.getPassword());
            }
            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("User not found with id " + id));
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id " + id));
        // Xoá teacher liên kết nếu có
        teacherRepository.deleteByUserId(id);
        // Xoá student liên kết nếu có
        studentRepository.deleteByUserId(id);
        // Xoá user cuối cùng
        userRepository.delete(user);
    }

    public List<User> getUsersByRole(String role) {
        return userRepository.findByRole(com.example.English.Center.Data.entity.users.UserRole.valueOf(role));
    }

    public List<User> getUsersByRoleAndStatus(String role, String status) {
        UserRole userRole = UserRole.valueOf(role.toUpperCase());
        return userRepository.findByRoleAndIsActiveTrue(userRole);
    }
}
