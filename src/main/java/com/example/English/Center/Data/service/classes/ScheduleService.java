package com.example.English.Center.Data.service.classes;

import com.example.English.Center.Data.dto.classes.ScheduleRequest;
import com.example.English.Center.Data.dto.classes.ScheduleResponse;
import com.example.English.Center.Data.entity.classes.Schedule;
import com.example.English.Center.Data.repository.classes.ScheduleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;

    public ScheduleService(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    public List<ScheduleResponse> getAll() {
        return scheduleRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public ScheduleResponse getById(Long id) {
        Schedule s = scheduleRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Schedule not found"));
        return toResponse(s);
    }

    public ScheduleResponse create(ScheduleRequest request) {
        Schedule s = Schedule.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        return toResponse(scheduleRepository.save(s));
    }

    public ScheduleResponse update(Long id, ScheduleRequest request) {
        Schedule s = scheduleRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Schedule not found"));
        s.setName(request.getName());
        s.setDescription(request.getDescription());
        return toResponse(scheduleRepository.save(s));
    }

    public void delete(Long id) {
        scheduleRepository.deleteById(id);
    }

    public List<ScheduleResponse> getSchedulesByStudentId(Long studentId) {
        return scheduleRepository.findByStudentId(studentId)
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    private ScheduleResponse toResponse(Schedule s) {
        ScheduleResponse res = new ScheduleResponse();
        res.setId(s.getId());
        res.setName(s.getName());
        res.setDescription(s.getDescription());
        return res;
    }
}
