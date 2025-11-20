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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, TeacherRepository teacherRepository, StudentRepository studentRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User createUser(UserRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // Encode password with BCrypt
        user.setRole(UserRole.valueOf(request.getRole()));
        user.setIsActive(true);
        user.setFullName(request.getFullName());
        return userRepository.save(user);
    }

    public User updateUser(Long id, UserRequest request) {
        return userRepository.findById(id).map(user -> {
            user.setUsername(request.getUsername());
            user.setRole(UserRole.valueOf(request.getRole()));
            if (request.getIsActive() != null) {
                user.setIsActive(request.getIsActive());
            }
            user.setFullName(request.getFullName());
            if (request.getPassword() != null && !request.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(request.getPassword())); // Encode with BCrypt
            }
            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("User not found"));
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

    // New: search users by name or username, optionally filter by role
    public List<User> searchUsers(String q, String role) {
        if (q == null || q.trim().isEmpty()) {
            if (role == null || role.isEmpty()) return userRepository.findAll();
            return getUsersByRole(role);
        }
        String key = q.trim();
        if (role == null || role.isEmpty()) {
            // search by fullName OR username (combine both lists, unique)
            List<User> byName = userRepository.findByFullNameContainingIgnoreCase(key);
            List<User> byUsername = userRepository.findByUsernameContainingIgnoreCase(key);
            // merge unique by id
            Map<Long, User> map = new LinkedHashMap<>();
            byName.forEach(u -> map.put(u.getId(), u));
            byUsername.forEach(u -> map.put(u.getId(), u));
            return new ArrayList<>(map.values());
        } else {
            UserRole userRole = UserRole.valueOf(role.toUpperCase());
            List<User> byName = userRepository.findByFullNameContainingIgnoreCaseAndRole(key, userRole);
            List<User> byUsername = userRepository.findByUsernameContainingIgnoreCaseAndRole(key, userRole);
            Map<Long, User> map = new LinkedHashMap<>();
            byName.forEach(u -> map.put(u.getId(), u));
            byUsername.forEach(u -> map.put(u.getId(), u));
            return new ArrayList<>(map.values());
        }
    }

    public LoginResponse login(LoginRequest request) {
        // Tìm user theo username
        User user = userRepository.findByUsername(request.getUsername()).orElse(null);

        // Nếu không tìm thấy user hoặc mật khẩu không đúng -> trả lỗi chung "sai thông tin đăng nhập"
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new com.example.English.Center.Data.exception.InvalidCredentialsException("Sai tên đăng nhập hoặc mật khẩu!");
        }

        // Kiểm tra tài khoản có bị khóa không (kiểm tra SAU khi đã xác thực username + password đúng)
        if (!user.getIsActive()) {
            throw new com.example.English.Center.Data.exception.AccountLockedException("Tài khoản đã bị khóa!");
        }

        // Kiểm tra role hợp lệ
        UserRole role = user.getRole();
        if (role == null || (!role.equals(UserRole.ADMIN) && !role.equals(UserRole.STUDENT) && !role.equals(UserRole.TEACHER))) {
            throw new RuntimeException("Tài khoản không có quyền truy cập hệ thống!");
        }

        // Use helper to build full UserResponse (includes student/teacher details when present)
        UserResponse response = toResponse(user);
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        return new LoginResponse(token, response);
    }

    // New helper: chuyển User -> UserResponse, populate student/teacher when present
    public UserResponse toResponse(User user) {
        if (user == null) return null;
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole() != null ? user.getRole().name() : null);
        response.setStatus(user.getIsActive() ? "ACTIVE" : "INACTIVE");
        response.setRoot(false);
        // Include the user's full name in the response
        response.setFullName(user.getFullName());
        if (user.getRole() != null) {
            switch (user.getRole()) {
                case STUDENT:
                    Optional<Student> optStudent = studentRepository.findByUserId(user.getId());
                    optStudent.ifPresent(s -> response.setStudent(new StudentResponse(s)));
                    break;
                case TEACHER:
                    Optional<Teacher> optTeacher = teacherRepository.findByUserId(user.getId());
                    optTeacher.ifPresent(t -> response.setTeacher(mapTeacherToResponse(t)));
                    break;
                default:
                    break;
            }
        }
        return response;
    }

    // Map Teacher entity to TeacherResponse to avoid constructor mismatch
    private TeacherResponse mapTeacherToResponse(Teacher t) {
        if (t == null) return null;
        TeacherResponse tr = new TeacherResponse();
        tr.setId(t.getId());
        tr.setUserId(t.getUser() != null ? t.getUser().getId() : null);
        tr.setFullName(t.getFullName());
        tr.setDob(t.getDob());
        tr.setGender(t.getGender());
        tr.setPhone(t.getPhone());
        tr.setAddress(t.getAddress());
        tr.setSpeciality(t.getSpeciality());
        tr.setHiredAt(t.getHiredAt());
        tr.setEmail(t.getEmail());
        return tr;
    }

    // New: get UserResponse from raw JWT token (expects 'token' without 'Bearer ')
    public UserResponse getUserFromToken(String token) {
        if (token == null || token.isEmpty()) throw new RuntimeException("Token missing");
        String username = jwtUtil.extractUsername(token);
        if (username == null || username.isEmpty()) throw new RuntimeException("Invalid token");
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found for token subject"));
        // validate token against username and expiration
        if (!jwtUtil.validateToken(token, user.getUsername())) {
            throw new RuntimeException("Token invalid or expired");
        }
        return toResponse(user);
    }

    // Change password for current user
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword, String confirmPassword) {
        // Validate inputs
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new com.example.English.Center.Data.exception.PasswordMismatchException("Mật khẩu mới không được để trống!");
        }
        if (newPassword.trim().length() < 6) {
            throw new com.example.English.Center.Data.exception.PasswordMismatchException("Mật khẩu mới phải có ít nhất 6 ký tự!");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new com.example.English.Center.Data.exception.PasswordMismatchException("Mật khẩu mới và xác nhận mật khẩu không khớp!");
        }

        // Find user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        // Verify current password using BCrypt
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new com.example.English.Center.Data.exception.InvalidCredentialsException("Mật khẩu hiện tại không đúng!");
        }

        // Check if new password is same as current (compare plain text before encoding)
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new com.example.English.Center.Data.exception.PasswordMismatchException("Mật khẩu mới phải khác mật khẩu hiện tại!");
        }

        // Update password with BCrypt encoding
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
