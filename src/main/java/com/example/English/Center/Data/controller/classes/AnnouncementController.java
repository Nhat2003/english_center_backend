package com.example.English.Center.Data.controller.classes;

import com.example.English.Center.Data.dto.announcement.AnnouncementRequest;
import com.example.English.Center.Data.dto.announcement.AnnouncementResponse;
import com.example.English.Center.Data.dto.announcement.NotificationResponse;
import com.example.English.Center.Data.entity.announcement.Announcement;
import com.example.English.Center.Data.entity.announcement.Notification;
import com.example.English.Center.Data.entity.classes.ClassRoom;
import com.example.English.Center.Data.entity.students.Student;
import com.example.English.Center.Data.entity.teachers.Teacher;
import com.example.English.Center.Data.entity.users.User;
import com.example.English.Center.Data.repository.announcement.NotificationRepository;
import com.example.English.Center.Data.repository.classes.ClassEntityRepository;
import com.example.English.Center.Data.repository.students.StudentRepository;
import com.example.English.Center.Data.repository.teachers.TeacherRepository;
import com.example.English.Center.Data.repository.users.UserRepository;
import com.example.English.Center.Data.service.announcement.AnnouncementService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/classes")
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final ClassEntityRepository classRepository;
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final NotificationRepository notificationRepository;

    public AnnouncementController(AnnouncementService announcementService,
                                  ClassEntityRepository classRepository,
                                  UserRepository userRepository,
                                  TeacherRepository teacherRepository,
                                  StudentRepository studentRepository,
                                  NotificationRepository notificationRepository) {
        this.announcementService = announcementService;
        this.classRepository = classRepository;
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.notificationRepository = notificationRepository;
    }

    // List announcements of a class (students/teachers/admin per security config)
    @GetMapping("/{classId}/announcements")
    public List<AnnouncementResponse> getClassAnnouncements(@PathVariable Long classId) {
        // ensure class exists
        classRepository.findById(classId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found"));
        return announcementService.getAnnouncementsByClass(classId).stream()
                .map(a -> new AnnouncementResponse(
                        a.getId(),
                        a.getClassRoom() != null ? a.getClassRoom().getId() : null,
                        a.getTitle(),
                        a.getContent(),
                        a.getCreatedAt(),
                        a.getCreatedBy() != null ? a.getCreatedBy().getId() : null
                )).collect(Collectors.toList());
    }

    // Teacher/Admin creates an announcement for a class; only assigned teacher allowed (unless admin)
    @PostMapping("/{classId}/announcements")
    public AnnouncementResponse createAnnouncement(@PathVariable Long classId, @RequestBody AnnouncementRequest payload) {
        ClassRoom clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        User user = userRepository.findByUsername(auth.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User not found"));
        boolean isAdmin = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_ADMIN"));

        Announcement ann = new Announcement();
        ann.setTitle(payload.getTitle());
        ann.setContent(payload.getContent());
        if (!isAdmin) {
            // must be the assigned teacher
            Teacher teacher = teacherRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher profile not found for user"));
            if (clazz.getTeacher() == null || !clazz.getTeacher().getId().equals(teacher.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the assigned teacher can post announcements for this class");
            }
            ann.setCreatedBy(teacher);
        }
        Announcement saved = announcementService.createAnnouncementForClass(classId, ann);
        return new AnnouncementResponse(
                saved.getId(),
                saved.getClassRoom() != null ? saved.getClassRoom().getId() : null,
                saved.getTitle(),
                saved.getContent(),
                saved.getCreatedAt(),
                saved.getCreatedBy() != null ? saved.getCreatedBy().getId() : null
        );
    }

    // Student reads their notifications (all classes or filter by classId)
    @GetMapping("/students/{studentId}/notifications")
    public List<NotificationResponse> getStudentNotifications(@PathVariable Long studentId,
                                                              @RequestParam(value = "classId", required = false) Long classId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        User user = userRepository.findByUsername(auth.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User not found"));
        boolean isAdmin = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_ADMIN"));
        boolean isStudent = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_STUDENT"));

        if (!isAdmin) {
            if (!isStudent) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
            Student me = studentRepository.findByUserId(user.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Student profile not found for user"));
            if (!me.getId().equals(studentId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Students can only view their own notifications");
            }
        }
        List<Notification> list = (classId != null)
                ? announcementService.getNotificationsForStudentInClass(classId, studentId)
                : announcementService.getNotificationsForStudent(studentId);
        return list.stream().map(n -> new NotificationResponse(
                n.getId(),
                n.getClassRoom() != null ? n.getClassRoom().getId() : null,
                n.getAnnouncement() != null ? n.getAnnouncement().getId() : null,
                n.getAnnouncement() != null ? n.getAnnouncement().getTitle() : null,
                n.getAnnouncement() != null ? n.getAnnouncement().getContent() : null,
                n.isReadFlag(),
                n.getCreatedAt(),
                n.getReadAt()
        )).collect(Collectors.toList());
    }

    // Mark a notification as read (student owner or admin)
    @PutMapping("/notifications/{notificationId}/read")
    public NotificationResponse markRead(@PathVariable Long notificationId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        User user = userRepository.findByUsername(auth.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User not found"));
        boolean isAdmin = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_ADMIN"));

        Notification current = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!isAdmin) {
            Student me = studentRepository.findByUserId(user.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Student profile not found for user"));
            if (current.getStudent() == null || !me.getId().equals(current.getStudent().getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Students can only mark their own notifications as read");
            }
        }
        Notification saved = announcementService.markRead(notificationId);
        return new NotificationResponse(
                saved.getId(),
                saved.getClassRoom() != null ? saved.getClassRoom().getId() : null,
                saved.getAnnouncement() != null ? saved.getAnnouncement().getId() : null,
                saved.getAnnouncement() != null ? saved.getAnnouncement().getTitle() : null,
                saved.getAnnouncement() != null ? saved.getAnnouncement().getContent() : null,
                saved.isReadFlag(),
                saved.getCreatedAt(),
                saved.getReadAt()
        );
    }
}
