package com.example.English.Center.Data.entity.teachers;

import com.example.English.Center.Data.entity.users.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "teachers")
public class Teacher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Quan hệ OneToOne với User
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true) // Khi xóa Teacher thì xóa luôn User
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String fullName;
    private LocalDate dob;
    private String gender;
    private String phone;
    private String address;
    private String speciality;
    private LocalDate hiredAt;
}
