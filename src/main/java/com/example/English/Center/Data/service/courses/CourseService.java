package com.example.English.Center.Data.service.courses;

import com.example.English.Center.Data.dto.courses.CourseRequest;
import com.example.English.Center.Data.dto.courses.CourseResponse;
import com.example.English.Center.Data.entity.courses.Course;
import com.example.English.Center.Data.repository.courses.CourseRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseService {
    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    public List<CourseResponse> getAll() {
        return courseRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public CourseResponse getById(Long id) {
        Course c = courseRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Course not found"));
        return toResponse(c);
    }

    public CourseResponse create(CourseRequest request) {
        Course c = Course.builder()
                .name(request.getName())
                .duration(request.getDuration())
                .fee(request.getFee())
                .description(request.getDescription())
                .build();
        return toResponse(courseRepository.save(c));
    }

    public CourseResponse update(Long id, CourseRequest request) {
        Course c = courseRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Course not found"));
        c.setName(request.getName());
        c.setDuration(request.getDuration());
        c.setFee(request.getFee());
        c.setDescription(request.getDescription());
        return toResponse(courseRepository.save(c));
    }

    public void delete(Long id) {
        courseRepository.deleteById(id);
    }

    private CourseResponse toResponse(Course c) {
        CourseResponse res = new CourseResponse();
        res.setId(c.getId());
        res.setName(c.getName());
        res.setDuration(c.getDuration());
        res.setFee(c.getFee());
        res.setDescription(c.getDescription());
        return res;
    }
}
