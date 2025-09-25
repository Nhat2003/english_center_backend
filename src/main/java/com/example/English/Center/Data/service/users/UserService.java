package com.example.English.Center.Data.service.users;

import com.example.English.Center.Data.config.JwtUtil;
import com.example.English.Center.Data.dto.LoginRequest;
import com.example.English.Center.Data.dto.LoginResponse;
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
    private final JwtUtil jwtUtil;

    public UserService(UserRepository userRepository, TeacherRepository teacherRepository, StudentRepository studentRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.jwtUtil = jwtUtil;
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

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Sai tên đăng nhập hoặc mật khẩu!"));
        if (!user.getPassword().equals(request.getPassword())) {
            throw new RuntimeException("Sai tên đăng nhập hoặc mật khẩu!");
        }
        if (!user.getIsActive()) {
            throw new RuntimeException("Tài khoản đã bị khóa!");
        }
        // Kiểm tra role hợp lệ
        UserRole role = user.getRole();
        if (role == null || (!role.equals(UserRole.ADMIN) && !role.equals(UserRole.STUDENT) && !role.equals(UserRole.TEACHER))) {
            throw new RuntimeException("Tài khoản không có quyền truy cập hệ thống!");
        }
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole() != null ? user.getRole().name() : null);
        response.setStatus(user.getIsActive() ? "ACTIVE" : "INACTIVE");
        response.setRoot(false);
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        return new LoginResponse(token, response);
    }
}
