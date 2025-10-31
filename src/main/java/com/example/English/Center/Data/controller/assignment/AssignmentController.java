package com.example.English.Center.Data.controller.assignment;

import com.example.English.Center.Data.entity.assignment.Assignment;
import com.example.English.Center.Data.entity.classes.ClassRoom;
import com.example.English.Center.Data.entity.teachers.Teacher;
import com.example.English.Center.Data.repository.classes.ClassEntityRepository;
import com.example.English.Center.Data.repository.teachers.TeacherRepository;
import com.example.English.Center.Data.service.assignment.AssignmentService;
import com.example.English.Center.Data.service.file.FileStorageService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final FileStorageService fileStorageService;
    private final ClassEntityRepository classEntityRepository;
    private final TeacherRepository teacherRepository;

    public AssignmentController(AssignmentService assignmentService, FileStorageService fileStorageService,
                                ClassEntityRepository classEntityRepository, TeacherRepository teacherRepository){
        this.assignmentService = assignmentService;
        this.fileStorageService = fileStorageService;
        this.classEntityRepository = classEntityRepository;
        this.teacherRepository = teacherRepository;
    }

    @GetMapping
    public List<Assignment> getAll(){
        return assignmentService.getAllAssignment();
    }

    @GetMapping("/{id}")
    public Assignment getById(@PathVariable Long id){
        return assignmentService.getById(id).orElse(null);
    }

    @GetMapping("/class/{classId}")
    public List<Assignment> getByClass(@PathVariable Long classId) {
        return assignmentService.getByClassId(classId);
    }

    // JSON endpoint (existing)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Assignment create(@RequestBody Assignment assignment) {
        return assignmentService.save(assignment);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        assignmentService.delete(id);
    }

    // New endpoint: create assignment with optional file upload (teacher creates assignment and file uploaded automatically)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Assignment createWithFile(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("classRoomId") Long classRoomId,
            @RequestParam("teacherId") Long teacherId,
            @RequestParam(value = "dueDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate
    ) throws Exception {
        String fileUrl = null;
        if (file != null && !file.isEmpty()) {
            fileUrl = fileStorageService.storeAssignmentFile(file);
        }

        Assignment assignment = new Assignment();
        assignment.setTitle(title);
        assignment.setDescription(description);
        assignment.setFileUrl(fileUrl);
        assignment.setDueDate(dueDate);

        ClassRoom classRoom = classEntityRepository.findById(classRoomId)
                .orElseThrow(() -> new RuntimeException("Class not found with id " + classRoomId));
        assignment.setClassRoom(classRoom);

        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found with id " + teacherId));
        assignment.setTeacher(teacher);

        return assignmentService.save(assignment);
    }

    // Existing upload endpoint (kept for compatibility)
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Assignment uploadAssignmentFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("classId") Long classId,
            @RequestParam("teacherId") Long teacherId,
            @RequestParam(value = "dueDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate
    ) throws Exception {
        String fileUrl = fileStorageService.storeAssignmentFile(file);

        Assignment assignment = new Assignment();
        assignment.setTitle(title);
        assignment.setDescription(description);
        assignment.setFileUrl(fileUrl);
        assignment.setDueDate(dueDate);

        ClassRoom classRoom = classEntityRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found with id " + classId));
        assignment.setClassRoom(classRoom);

        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found with id " + teacherId));
        assignment.setTeacher(teacher);

        return assignmentService.save(assignment);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Assignment updateAssignmentWithFile(
            @PathVariable Long id,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("classRoomId") Long classRoomId,
            @RequestParam("teacherId") Long teacherId,
            @RequestParam(value = "dueDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate
    ) throws Exception {
        String fileUrl = null;
        if (file != null && !file.isEmpty()) {
            fileUrl = fileStorageService.storeAssignmentFile(file);
        }

        Assignment assignment = assignmentService.getById(id).orElseThrow(() -> new RuntimeException("Assignment not found with id " + id));
        assignment.setTitle(title);
        assignment.setDescription(description);
        assignment.setFileUrl(fileUrl);
        assignment.setDueDate(dueDate);

        ClassRoom classRoom = classEntityRepository.findById(classRoomId)
                .orElseThrow(() -> new RuntimeException("Class not found with id " + classRoomId));
        assignment.setClassRoom(classRoom);

        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found with id " + teacherId));
        assignment.setTeacher(teacher);

        return assignmentService.save(assignment);
    }
}
