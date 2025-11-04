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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.example.English.Center.Data.repository.students.StudentRepository;
import com.example.English.Center.Data.repository.users.UserRepository;
import com.example.English.Center.Data.entity.users.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.http.MediaTypeFactory;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final FileStorageService fileStorageService;
    private final ClassEntityRepository classEntityRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public AssignmentController(AssignmentService assignmentService, FileStorageService fileStorageService,
                                ClassEntityRepository classEntityRepository, TeacherRepository teacherRepository,
                                StudentRepository studentRepository, UserRepository userRepository){
        this.assignmentService = assignmentService;
        this.fileStorageService = fileStorageService;
        this.classEntityRepository = classEntityRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
    }

    @Value("${upload.assignment-dir}")
    private String assignmentDir;

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
        // enforce authorization: only students in class, assigned teacher, or admin can view
        var classOpt = classEntityRepository.findById(classId);
        if (classOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found with id " + classId);
        }
        var classRoom = classOpt.get();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        boolean isAdmin = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_ADMIN"));
        if (isAdmin) {
            return assignmentService.getByClassId(classId);
        }

        // try teacher
        boolean isTeacherRole = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_TEACHER"));
        if (isTeacherRole) {
            // find teacher by username
            User user = userRepository.findByUsername(auth.getName()).orElse(null);
            if (user != null) {
                var teacherOpt = teacherRepository.findByUserId(user.getId());
                if (teacherOpt.isPresent() && classRoom.getTeacher() != null && teacherOpt.get().getId().equals(classRoom.getTeacher().getId())) {
                    return assignmentService.getByClassId(classId);
                }
            }
        }

        // try student
        boolean isStudentRole = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_STUDENT"));
        if (isStudentRole) {
            User user = userRepository.findByUsername(auth.getName()).orElse(null);
            if (user != null) {
                var studentOpt = studentRepository.findByUserId(user.getId());
                if (studentOpt.isPresent()) {
                    var student = studentOpt.get();
                    // check membership
                    if (classRoom.getStudents() != null && classRoom.getStudents().stream().anyMatch(s -> s.getId().equals(student.getId()))) {
                        return assignmentService.getByClassId(classId);
                    }
                }
            }
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: you are not allowed to view assignments for this class");
    }

    // New safe endpoint: get assignments for the authenticated student (no studentId param)
    @GetMapping("/me")
    public List<Assignment> getMyAssignments() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        User user = userRepository.findByUsername(auth.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User not found"));

        boolean isAdmin = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_ADMIN"));
        if (isAdmin) {
            return assignmentService.getAllAssignment();
        }

        boolean isTeacher = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_TEACHER"));
        if (isTeacher) {
            // find teacher entity and classes taught
            var teacherOpt = teacherRepository.findByUserId(user.getId());
            if (teacherOpt.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher profile not found for user");
            Long teacherId = teacherOpt.get().getId();
            var classes = classEntityRepository.findByTeacher_Id(teacherId);
            return classes.stream().flatMap(c -> assignmentService.getByClassId(c.getId()).stream()).toList();
        }

        // default: student
        var studentOpt = studentRepository.findByUserId(user.getId());
        if (studentOpt.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student profile not found for user");
        Long studentId = studentOpt.get().getId();
        List<ClassRoom> classes = classEntityRepository.findByStudents_Id(studentId);
        return classes.stream().flatMap(c -> assignmentService.getByClassId(c.getId()).stream()).toList();
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

    // Secure download endpoint for assignment file
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadAssignmentFile(@PathVariable Long id) {
        java.util.Optional<Assignment> opt = assignmentService.getById(id);
        if (!opt.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found with id " + id);
        }
        Assignment assignment = opt.get();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        boolean allowed = false;
        // check admin
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if ("ROLE_ADMIN".equals(ga.getAuthority())) {
                allowed = true;
                break;
            }
        }

        // check teacher
        if (!allowed) {
            for (GrantedAuthority ga : auth.getAuthorities()) {
                if ("ROLE_TEACHER".equals(ga.getAuthority())) {
                    User user = userRepository.findByUsername(auth.getName()).orElse(null);
                    if (user != null) {
                        java.util.Optional<Teacher> tOpt = teacherRepository.findByUserId(user.getId());
                        if (tOpt.isPresent() && assignment.getTeacher() != null && tOpt.get().getId().equals(assignment.getTeacher().getId())) {
                            allowed = true;
                            break;
                        }
                    }
                }
            }
        }

        // check student membership
        if (!allowed) {
            for (GrantedAuthority ga : auth.getAuthorities()) {
                if ("ROLE_STUDENT".equals(ga.getAuthority())) {
                    User user = userRepository.findByUsername(auth.getName()).orElse(null);
                    if (user != null) {
                        java.util.Optional<com.example.English.Center.Data.entity.students.Student> sOpt = studentRepository.findByUserId(user.getId());
                        if (sOpt.isPresent() && assignment.getClassRoom() != null && assignment.getClassRoom().getStudents() != null) {
                            for (com.example.English.Center.Data.entity.students.Student s : assignment.getClassRoom().getStudents()) {
                                if (s.getId().equals(sOpt.get().getId())) {
                                    allowed = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (allowed) break;
            }
        }

        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to download this assignment file");
        }

        String fileUrl = assignment.getFileUrl();
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No file attached for this assignment");
        }

        String filename = StringUtils.getFilename(fileUrl);
        if (filename == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }

        try {
            Path filePath = Paths.get(assignmentDir).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found on server");
            }

            org.springframework.http.MediaType mediaType = MediaTypeFactory.getMediaType(resource).orElse(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (java.net.MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read file", e);
        }
    }
}
