package com.example.English.Center.Data.dto.students;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class StudentRequest {
    @NotNull(message = "User ID is required")
    private Long userId;

    // All other fields are optional - can be filled later
    @Size(max = 100, message = "Full name must be at most 100 characters")
    private String fullName;

    private String className;

    private LocalDate dob;

    private String gender;

    @Pattern(regexp = "^\\d{10,15}$", message = "Phone must be 10-15 digits", groups = {})
    private String phone;

    private String address;

    private LocalDate joinedAt;

    @Email(message = "Email should be valid")
    private String email;

    // Getters & Setters
    public Long getUserId() {
        return userId;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDate joinedAt) {
        this.joinedAt = joinedAt;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}
