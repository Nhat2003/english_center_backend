package com.example.English.Center.Data.service.assignment;
import com.example.English.Center.Data.entity.assignment.Assignment;
import com.example.English.Center.Data.repository.assignment.AssignmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;

    public AssignmentService(AssignmentRepository assignmentRepository){
        this.assignmentRepository = assignmentRepository;
    }

    public List<Assignment> getAllAssignment(){
        return assignmentRepository.findAll();
    }

    public Optional<Assignment> getById(Long id){
        return assignmentRepository.findById(id);
    }

    public List<Assignment> getByClassId(Long classId) {
        return assignmentRepository.findByClassRoomId(classId);
    }

    public Assignment save(Assignment assignment) {
        return assignmentRepository.save(assignment);
    }

    public void delete(Long id) {
        assignmentRepository.deleteById(id);
    }

}
