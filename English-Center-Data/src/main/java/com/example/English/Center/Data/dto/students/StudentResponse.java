package com.example.English.Center.Data.dto.students;

import com.example.English.Center.Data.entity.students.Student;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StudentResponse {
    private Long id;
    private Long userId;
    private String fullName;
    private LocalDate dob;
    private String gender;
    private String phone;
    private String address;
    private LocalDate joinedAt;

    public StudentResponse(Student student) {
        this.id = student.getId();
        this.userId = student.getUser() != null ? student.getUser().getId() : null;
        this.fullName = student.getFullName();
        this.dob = student.getDob();
        this.gender = student.getGender();
        this.phone = student.getPhone();
        this.address = student.getAddress();
        this.joinedAt = student.getJoinedAt();
    }
}

