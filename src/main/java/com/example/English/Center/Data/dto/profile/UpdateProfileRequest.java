package com.example.English.Center.Data.dto.profile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileRequest {
    private String fullName;
    private LocalDate dob;
    private String gender;
    @Pattern(regexp = "^\\d{10,15}$", message = "Phone must be 10-15 digits")
    private String phone;
    private String address;
    @Email(message = "Email should be valid")
    private String email;
    // For teachers only
    private String speciality;
}

