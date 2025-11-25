package com.example.English.Center.Data.dto.classes;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor


public class FixedScheduleDTO {
    private String name;
    @JsonAlias({"dayOfWeek", "daysOfWeek"})
    private String daysOfWeek; // ví dụ: "2,4,6"
    private String startTime;  // ví dụ: "18:00:00"
    private String endTime;    // ví dụ: "20:00:00"
}
