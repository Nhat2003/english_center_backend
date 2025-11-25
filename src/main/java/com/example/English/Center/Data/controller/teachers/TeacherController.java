package com.example.English.Center.Data.controller.teachers;

import com.example.English.Center.Data.dto.ChangePasswordRequest;
import com.example.English.Center.Data.dto.SuccessResponse;
import com.example.English.Center.Data.dto.profile.UpdateProfileRequest;
import com.example.English.Center.Data.dto.teachers.TeacherRequest;
import com.example.English.Center.Data.dto.teachers.TeacherResponse;
import com.example.English.Center.Data.entity.teachers.Teacher;
import com.example.English.Center.Data.entity.users.User;
import com.example.English.Center.Data.repository.teachers.TeacherRepository;
import com.example.English.Center.Data.repository.users.UserRepository;
import com.example.English.Center.Data.service.teachers.TeacherService;
import com.example.English.Center.Data.service.users.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/teachers")
public class TeacherController {

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private UserService userService;

    @GetMapping
    public List<TeacherResponse> getAllTeachers() {
        return teacherService.getAllTeachers();
    }

    @GetMapping("/{id}")
    public TeacherResponse getTeacherById(@PathVariable Long id) {
        return teacherService.getTeacherById(id);
    }

    @PostMapping
    public TeacherResponse createTeacher(@RequestBody TeacherRequest request) {
        return teacherService.createTeacher(request);
    }

    @PutMapping("/{id}")
    public TeacherResponse updateTeacher(@PathVariable Long id, @RequestBody TeacherRequest request) {
        return teacherService.updateTeacher(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteTeacher(@PathVariable Long id) {
        teacherService.deleteTeacher(id);
    }

    @GetMapping("/me/profile")
    public ResponseEntity<TeacherResponse> getMyProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Not authenticated");
        }
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "User not found"));
        Teacher me = teacherRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Teacher profile not found"));

        return ResponseEntity.ok(teacherService.getTeacherById(me.getId()));
    }

    @PutMapping("/me/profile")
    public ResponseEntity<TeacherResponse> updateMyProfile(@RequestBody @Valid UpdateProfileRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Not authenticated");
        }
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "User not found"));
        Teacher me = teacherRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Teacher profile not found"));

        TeacherResponse updated = teacherService.updateMyProfile(me.getId(), request);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/me/change-password")
    public ResponseEntity<SuccessResponse> changeMyPassword(@RequestBody @Valid ChangePasswordRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Not authenticated");
        }
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "User not found"));
        Teacher me = teacherRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Teacher profile not found"));

        // UserService will handle validation and password change
        userService.changePassword(user.getId(), request.getCurrentPassword(), request.getNewPassword(), request.getConfirmPassword());

        return ResponseEntity.ok(new SuccessResponse("Đổi mật khẩu thành công!"));
    }
}
