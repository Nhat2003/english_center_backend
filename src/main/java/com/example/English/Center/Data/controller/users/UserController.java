package com.example.English.Center.Data.controller.users;

import com.example.English.Center.Data.dto.LoginRequest;
import com.example.English.Center.Data.dto.LoginResponse;
import com.example.English.Center.Data.dto.users.UserRequest;
import com.example.English.Center.Data.dto.users.UserResponse;
import com.example.English.Center.Data.entity.users.User;
import com.example.English.Center.Data.repository.users.UserRepository;
import com.example.English.Center.Data.service.users.UserService;
import com.example.English.Center.Data.repository.students.StudentRepository;
import com.example.English.Center.Data.repository.teachers.TeacherRepository;
import com.example.English.Center.Data.repository.classes.ClassEntityRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ClassEntityRepository classRepository;
    private final UserRepository userRepository;

    public UserController(UserService userService,
                         StudentRepository studentRepository,
                         TeacherRepository teacherRepository,
                         ClassEntityRepository classRepository,
                         UserRepository userRepository) {
        this.userService = userService;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.classRepository = classRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers().stream()
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getRole() != null ? user.getRole().name() : null,
                        user.getIsActive() ? "ACTIVE" : "INACTIVE",
                        false,
                        null,
                        null,
                        user.getFullName()
                )).toList();
    }

    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getIsActive() ? "ACTIVE" : "INACTIVE",
                false,
                null,
                null,
                user.getFullName()
        );
    }

    @PostMapping
    public UserResponse createUser(@RequestBody UserRequest request) {
        User user = userService.createUser(request);
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getIsActive() ? "ACTIVE" : "INACTIVE",
                false,
                null,
                null,
                user.getFullName()
        );
    }

    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable Long id, @RequestBody UserRequest request) {
        User user = userService.updateUser(id, request);
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getIsActive() ? "ACTIVE" : "INACTIVE",
                false,
                null,
                null,
                user.getFullName()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("Deleted user (and student if exists) with id " + id);
    }

    @GetMapping(params = "role")
    public List<User> getUsersByRole(@RequestParam String role) {
        return userService.getUsersByRole(role);
    }

    @GetMapping(params = {"role", "status"})
    public List<User> getUsersByRoleAndStatus(@RequestParam String role, @RequestParam String status) {
        return userService.getUsersByRoleAndStatus(role, status);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        // Chức năng logout với JWT chỉ cần frontend xoá token, backend trả về thành công
        return ResponseEntity.ok("Logout successful");
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserResponse>> searchUsers(@RequestParam(required = false) String q,
                                          @RequestParam(required = false) String role,
                                          @RequestParam(required = false) Long classId) {
        // Authentication check
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        String username = auth.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User not found"));

        boolean isAdmin = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN"));

        List<User> results;

        // If classId provided, search only within that class (for scoped chat search)
        if (classId != null) {
            var classOpt = classRepository.findById(classId);
            if (classOpt.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found");
            }

            var classRoom = classOpt.get();
            Set<Long> allowedUserIds = new java.util.HashSet<>();

            // Verify user is in this class
            boolean userInClass = false;
            if (isAdmin) {
                userInClass = true;
            } else {
                var studentOpt = studentRepository.findByUserId(currentUser.getId());
                if (studentOpt.isPresent() && classRoom.getStudents() != null) {
                    userInClass = classRoom.getStudents().stream()
                            .anyMatch(s -> s.getId().equals(studentOpt.get().getId()));
                }
                if (!userInClass) {
                    var teacherOpt = teacherRepository.findByUserId(currentUser.getId());
                    if (teacherOpt.isPresent() && classRoom.getTeacher() != null) {
                        userInClass = classRoom.getTeacher().getId().equals(teacherOpt.get().getId());
                    }
                }
            }

            if (!userInClass && !isAdmin) {
                return ResponseEntity.status(403).body(List.of()); // Not in this class
            }

            // Collect users from this class only
            if (classRoom.getTeacher() != null && classRoom.getTeacher().getUser() != null) {
                allowedUserIds.add(classRoom.getTeacher().getUser().getId());
            }
            if (classRoom.getStudents() != null) {
                classRoom.getStudents().stream()
                        .filter(s -> s.getUser() != null)
                        .forEach(s -> allowedUserIds.add(s.getUser().getId()));
            }

            // Always add active admins
            var admins = userRepository.findByRoleAndIsActiveTrue(com.example.English.Center.Data.entity.users.UserRole.ADMIN);
            for (var admin : admins) {
                allowedUserIds.add(admin.getId());
            }

            // Get search results and filter by allowed users in this class
            results = userService.searchUsers(q, role).stream()
                    .filter(user -> allowedUserIds.contains(user.getId()))
                    .collect(Collectors.toList());
        } else {
            // Original logic: search all contacts user can reach (no classId specified)
            if (isAdmin) {
                // Admins can search all users
                results = userService.searchUsers(q, role);
            } else {
                // Students and teachers can search users in their classes + active admins
                boolean isStudent = auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(a -> a.equals("ROLE_STUDENT"));

                boolean isTeacher = auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(a -> a.equals("ROLE_TEACHER"));

                Set<Long> allowedUserIds = new java.util.HashSet<>();

                if (isStudent) {
                    // Get classes student is in, collect all user ids (teacher + other students)
                    var studentOpt = studentRepository.findByUserId(currentUser.getId());
                    if (studentOpt.isPresent()) {
                        var classes = classRepository.findByStudents_Id(studentOpt.get().getId());
                        for (var cr : classes) {
                            if (cr.getTeacher() != null && cr.getTeacher().getUser() != null) {
                                allowedUserIds.add(cr.getTeacher().getUser().getId());
                            }
                            if (cr.getStudents() != null) {
                                cr.getStudents().stream()
                                        .filter(s -> s.getUser() != null)
                                        .forEach(s -> allowedUserIds.add(s.getUser().getId()));
                            }
                        }
                    }
                } else if (isTeacher) {
                    // Get classes teacher is in, collect all user ids (students)
                    var teacherOpt = teacherRepository.findByUserId(currentUser.getId());
                    if (teacherOpt.isPresent()) {
                        var classes = classRepository.findByTeacher_Id(teacherOpt.get().getId());
                        for (var cr : classes) {
                            if (cr.getStudents() != null) {
                                cr.getStudents().stream()
                                        .filter(s -> s.getUser() != null)
                                        .forEach(s -> allowedUserIds.add(s.getUser().getId()));
                            }
                        }
                    }
                }

                // Always allow active admins
                var admins = userRepository.findByRoleAndIsActiveTrue(com.example.English.Center.Data.entity.users.UserRole.ADMIN);
                for (var admin : admins) {
                    allowedUserIds.add(admin.getId());
                }

                // Get all search results and filter by allowed users
                results = userService.searchUsers(q, role).stream()
                        .filter(user -> allowedUserIds.contains(user.getId()))
                        .collect(Collectors.toList());
            }
        }

        return ResponseEntity.ok(results.stream().map(user -> new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getIsActive() ? "ACTIVE" : "INACTIVE",
                false,
                null,
                null,
                user.getFullName()
        )).toList());
    }
}
