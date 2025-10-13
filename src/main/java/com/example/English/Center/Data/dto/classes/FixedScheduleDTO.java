package com.example.English.Center.Data.dto.classes;

public class FixedScheduleDTO {
    private String name;
    private String daysOfWeek; // ví dụ: "2,4,6"
    private String startTime;  // ví dụ: "18:00:00"
    private String endTime;    // ví dụ: "20:00:00"

    public FixedScheduleDTO() {}
    public FixedScheduleDTO(String name, String daysOfWeek, String startTime, String endTime) {
        this.name = name;
        this.daysOfWeek = daysOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(String daysOfWeek) { this.daysOfWeek = daysOfWeek; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}

