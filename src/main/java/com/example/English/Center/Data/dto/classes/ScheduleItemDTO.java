package com.example.English.Center.Data.dto.classes;

public class ScheduleItemDTO {
    private String id;
    private String title;
    private String start;
    private String end;
    private Integer classId;
    private Integer teacherId;
    private Integer roomId;

    public ScheduleItemDTO() {}

    public ScheduleItemDTO(String id, String title, String start, String end, Integer classId, Integer teacherId, Integer roomId) {
        this.id = id;
        this.title = title;
        this.start = start;
        this.end = end;
        this.classId = classId;
        this.teacherId = teacherId;
        this.roomId = roomId;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStart() { return start; }
    public void setStart(String start) { this.start = start; }
    public String getEnd() { return end; }
    public void setEnd(String end) { this.end = end; }
    public Integer getClassId() { return classId; }
    public void setClassId(Integer classId) { this.classId = classId; }
    public Integer getTeacherId() { return teacherId; }
    public void setTeacherId(Integer teacherId) { this.teacherId = teacherId; }
    public Integer getRoomId() { return roomId; }
    public void setRoomId(Integer roomId) { this.roomId = roomId; }
}

