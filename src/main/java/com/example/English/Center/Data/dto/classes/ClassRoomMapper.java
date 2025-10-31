package com.example.English.Center.Data.dto.classes;

import com.example.English.Center.Data.entity.classes.ClassRoom;
import com.example.English.Center.Data.entity.classes.FixedSchedule;
import com.example.English.Center.Data.entity.students.Student;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

public class ClassRoomMapper {
    public static ClassRoomResponse toResponse(ClassRoom classRoom) {
        ClassRoomResponse response = new ClassRoomResponse();
        response.setId(classRoom.getId());
        response.setName(classRoom.getName());
        response.setCourseName(classRoom.getCourse().getName());
        response.setTeacherName(classRoom.getTeacher().getFullName());
        response.setRoomName(classRoom.getRoom().getName());
        // Map fixedSchedule
        FixedSchedule fs = classRoom.getFixedSchedule();
        if (fs != null) {
            ClassRoomResponse.FixedScheduleInfo fsInfo = new ClassRoomResponse.FixedScheduleInfo();
            fsInfo.setName(fs.getName());
            fsInfo.setDaysOfWeek(fs.getDaysOfWeek());
            fsInfo.setStartTime(fs.getStartTime());
            fsInfo.setEndTime(fs.getEndTime());
            response.setFixedSchedule(fsInfo);
        }
        response.setStartDate(classRoom.getStartDate().toString());
        response.setEndDate(classRoom.getEndDate().toString());
        // Map danh sách học sinh
        if (classRoom.getStudents() != null) {
            Set<Long> studentIds = classRoom.getStudents().stream().map(Student::getId).collect(Collectors.toCollection(LinkedHashSet::new));
            response.setStudents(studentIds);
            response.setStudentCount(studentIds.size());
        } else {
            response.setStudents(Set.of());
            response.setStudentCount(0);
        }
        return response;
    }
}
