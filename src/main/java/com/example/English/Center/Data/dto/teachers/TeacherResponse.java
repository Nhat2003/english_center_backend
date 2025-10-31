package com.example.English.Center.Data.dto.teachers;

import lombok.Data;
import java.time.LocalDate;

@Data
public class TeacherResponse {
    private Long id;
    private Long userId;
    private String fullName;
    private LocalDate dob;
    private String gender;
    private String phone;
    private String address;
    private String speciality;
    private LocalDate hiredAt;
    private String email;
}

