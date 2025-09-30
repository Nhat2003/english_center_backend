package com.example.English.Center.Data.dto.users;

import com.example.English.Center.Data.dto.students.StudentResponse;
import com.example.English.Center.Data.dto.teachers.TeacherResponse;
import com.example.English.Center.Data.entity.users.UserRole;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    private Long id;
    private String username;
    private String role;
    private String status;
    private boolean root;
    private String fullName;

    private StudentResponse student;
    private TeacherResponse teacher;
}
