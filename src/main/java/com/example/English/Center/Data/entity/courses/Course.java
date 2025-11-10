package com.example.English.Center.Data.entity.courses;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer duration; // số buổi/giờ

    @Column(nullable = false)
    private BigDecimal fee;

    // Mô tả khóa học (optional)
    @Column(columnDefinition = "text")
    private String description;
}
