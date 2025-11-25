package com.example.English.Center.Data.dto.profile;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileRequest {
    private String fullName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Past(message = "Date of birth must be in the past")
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
