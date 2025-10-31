package com.example.English.Center.Data.controller.users;

import com.example.English.Center.Data.dto.LoginRequest;
import com.example.English.Center.Data.dto.LoginResponse;
import com.example.English.Center.Data.dto.users.UserRequest;
import com.example.English.Center.Data.dto.users.UserResponse;
import com.example.English.Center.Data.entity.users.User;
import com.example.English.Center.Data.service.users.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
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
                        null
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
                null
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
                null
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
                null
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
}
