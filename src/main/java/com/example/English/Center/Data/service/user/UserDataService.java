package com.example.English.Center.Data.service.user;

import com.example.English.Center.Data.entity.classes.ClassRoom;
import com.example.English.Center.Data.entity.classes.FixedSchedule;
import com.example.English.Center.Data.entity.submission.Submission;
import com.example.English.Center.Data.repository.classes.ClassEntityRepository;
import com.example.English.Center.Data.repository.submission.SubmissionRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserDataService {
    private final ClassEntityRepository classEntityRepository;
    private final SubmissionRepository submissionRepository;

    public UserDataService(ClassEntityRepository classEntityRepository, SubmissionRepository submissionRepository) {
        this.classEntityRepository = classEntityRepository;
        this.submissionRepository = submissionRepository;
    }

    public Optional<List<Map<String, Object>>> getScheduleForUser(Long userId) {
        List<ClassRoom> classes = classEntityRepository.findByStudents_Id(userId);
        if (classes == null || classes.isEmpty()) return Optional.of(List.of());

        List<Map<String,Object>> schedule = new ArrayList<>();
        for (ClassRoom cr : classes) {
            Map<String,Object> item = new HashMap<>();
            item.put("classId", cr.getId());
            item.put("className", cr.getName());
            item.put("startDate", cr.getStartDate() == null ? null : cr.getStartDate().toString());
            item.put("endDate", cr.getEndDate() == null ? null : cr.getEndDate().toString());
            if (cr.getFixedSchedule() != null) {
                FixedSchedule fs = cr.getFixedSchedule();
                item.put("scheduleName", fs.getName());
                item.put("daysOfWeek", fs.getDaysOfWeek());
                item.put("startTime", fs.getStartTime());
                item.put("endTime", fs.getEndTime());
            }
            if (cr.getTeacher() != null && cr.getTeacher().getUser() != null) {
                item.put("teacher", cr.getTeacher().getUser().getFullName());
            }
            schedule.add(item);
        }
        return Optional.of(schedule);
    }

    public Optional<List<Map<String, Object>>> getGradesForUser(Long userId) {
        List<Submission> subs = submissionRepository.findByStudentId(userId);
        if (subs == null || subs.isEmpty()) return Optional.of(List.of());

        List<Map<String,Object>> grades = new ArrayList<>();
        for (Submission s : subs) {
            Map<String,Object> item = new HashMap<>();
            if (s.getAssignment() != null) {
                item.put("assignmentId", s.getAssignment().getId());
                item.put("assignmentTitle", s.getAssignment().getTitle());
                if (s.getAssignment().getClassRoom() != null) item.put("className", s.getAssignment().getClassRoom().getName());
            }
            item.put("grade", s.getGrade());
            item.put("feedback", s.getFeedback());
            item.put("submittedAt", s.getSubmittedAt() == null ? null : s.getSubmittedAt().toString());
            grades.add(item);
        }
        return Optional.of(grades);
    }
}
