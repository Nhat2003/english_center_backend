package com.example.English.Center.Data.dto.teachers;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class TeacherRequest {
    @NotNull(message = "User ID is required")
    private Long userId;

    // All other fields are optional - can be filled later
    private String fullName;
    private LocalDate dob;
    private String gender;
    private String phone;
    private String address;
    private String speciality;
    private LocalDate hiredAt;
    private String email;
    // Bank info
    private String bankName;
    private String bankAccountNumber;

 }
