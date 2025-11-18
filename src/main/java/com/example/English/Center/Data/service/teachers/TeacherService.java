package com.example.English.Center.Data.service.teachers;

import com.example.English.Center.Data.dto.profile.UpdateProfileRequest;
import com.example.English.Center.Data.dto.teachers.TeacherRequest;
import com.example.English.Center.Data.dto.teachers.TeacherResponse;
import com.example.English.Center.Data.entity.teachers.Teacher;
import com.example.English.Center.Data.entity.users.User;
import com.example.English.Center.Data.repository.teachers.TeacherRepository;
import com.example.English.Center.Data.repository.users.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeacherService {
    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private UserRepository userRepository;

    public List<TeacherResponse> getAllTeachers() {
        return teacherRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public TeacherResponse getTeacherById(Long id) {
        return teacherRepository.findById(id).map(this::mapToResponse).orElse(null);
    }

    public TeacherResponse createTeacher(TeacherRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (teacherRepository.findByUserId(request.getUserId()).isPresent()) {
            throw new RuntimeException("UserId này đã được liên kết với một giáo viên khác");
        }
        Teacher teacher = Teacher.builder()
                .user(user)
                .fullName(request.getFullName())
                .dob(request.getDob())
                .gender(request.getGender())
                .phone(request.getPhone())
                .address(request.getAddress())
                .speciality(request.getSpeciality())
                .hiredAt(request.getHiredAt())
                .email(request.getEmail())
                .build();

        return mapToResponse(teacherRepository.save(teacher));
    }

    public TeacherResponse updateTeacher(Long id, TeacherRequest request) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        teacher.setFullName(request.getFullName());
        teacher.setDob(request.getDob());
        teacher.setGender(request.getGender());
        teacher.setPhone(request.getPhone());
        teacher.setAddress(request.getAddress());
        teacher.setSpeciality(request.getSpeciality());
        teacher.setHiredAt(request.getHiredAt());
        teacher.setEmail(request.getEmail());

        return mapToResponse(teacherRepository.save(teacher));
    }

    public void deleteTeacher(Long id) {
        teacherRepository.deleteById(id); // Xoá teacher → xoá luôn user vì cascade
    }

    // Teacher updates their own profile
    public TeacherResponse updateMyProfile(Long teacherId, UpdateProfileRequest request) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new EntityNotFoundException("Teacher not found with id " + teacherId));

        if (request.getFullName() != null) {
            teacher.setFullName(request.getFullName());
        }
        if (request.getDob() != null) {
            teacher.setDob(request.getDob());
        }
        if (request.getGender() != null) {
            teacher.setGender(request.getGender());
        }
        if (request.getPhone() != null) {
            teacher.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            teacher.setAddress(request.getAddress());
        }
        if (request.getEmail() != null) {
            teacher.setEmail(request.getEmail());
        }
        if (request.getSpeciality() != null) {
            teacher.setSpeciality(request.getSpeciality());
        }

        return mapToResponse(teacherRepository.save(teacher));
    }

    private TeacherResponse mapToResponse(Teacher teacher) {
        TeacherResponse response = new TeacherResponse();
        response.setId(teacher.getId());
        response.setUserId(teacher.getUser().getId());
        response.setFullName(teacher.getFullName());
        response.setDob(teacher.getDob());
        response.setGender(teacher.getGender());
        response.setPhone(teacher.getPhone());
        response.setAddress(teacher.getAddress());
        response.setSpeciality(teacher.getSpeciality());
        response.setHiredAt(teacher.getHiredAt());
        response.setEmail(teacher.getEmail());
        return response;
    }
}
