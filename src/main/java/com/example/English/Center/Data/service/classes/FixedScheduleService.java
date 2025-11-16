package com.example.English.Center.Data.service.classes;

import com.example.English.Center.Data.entity.classes.FixedSchedule;
import com.example.English.Center.Data.repository.classes.FixedScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FixedScheduleService {
    @Autowired
    private FixedScheduleRepository fixedScheduleRepository;

    public List<FixedSchedule> getAll() {
        return fixedScheduleRepository.findAll();
    }

    public Optional<FixedSchedule> getById(Long id) {
        return fixedScheduleRepository.findById(id);
    }

    public FixedSchedule create(FixedSchedule fixedSchedule) {
        return fixedScheduleRepository.save(fixedSchedule);
    }

    public FixedSchedule update(Long id, FixedSchedule fixedSchedule) {
        fixedSchedule.setId(id);
        return fixedScheduleRepository.save(fixedSchedule);
    }

    public void delete(Long id) {
        fixedScheduleRepository.deleteById(id);
    }
}

