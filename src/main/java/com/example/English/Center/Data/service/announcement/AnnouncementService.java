package com.example.English.Center.Data.service.announcement;

import com.example.English.Center.Data.entity.announcement.Announcement;
import com.example.English.Center.Data.entity.announcement.Notification;
import com.example.English.Center.Data.entity.classes.ClassRoom;
import com.example.English.Center.Data.entity.students.Student;
import com.example.English.Center.Data.repository.announcement.AnnouncementRepository;
import com.example.English.Center.Data.repository.announcement.NotificationRepository;
import com.example.English.Center.Data.repository.classes.ClassEntityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AnnouncementService {
    private final AnnouncementRepository announcementRepository;
    private final NotificationRepository notificationRepository;
    private final ClassEntityRepository classRepository;

    public AnnouncementService(AnnouncementRepository announcementRepository,
                               NotificationRepository notificationRepository,
                               ClassEntityRepository classRepository) {
        this.announcementRepository = announcementRepository;
        this.notificationRepository = notificationRepository;
        this.classRepository = classRepository;
    }

    public List<Announcement> getAnnouncementsByClass(Long classId) {
        return announcementRepository.findByClassRoom_IdOrderByCreatedAtDesc(classId);
    }

    @Transactional
    public Announcement createAnnouncementForClass(Long classId, Announcement announcement) {
        ClassRoom classRoom = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Class not found: " + classId));
        announcement.setId(null);
        announcement.setClassRoom(classRoom);
        announcement.setCreatedAt(LocalDateTime.now());
        Announcement saved = announcementRepository.save(announcement);

        // Fan-out notifications to all students in the class
        if (classRoom.getStudents() != null) {
            List<Notification> batch = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            for (Student s : classRoom.getStudents()) {
                Notification n = Notification.builder()
                        .announcement(saved)
                        .student(s)
                        .classRoom(classRoom)
                        .createdAt(now)
                        .readFlag(false)
                        .build();
                batch.add(n);
            }
            notificationRepository.saveAll(batch);
        }
        return saved;
    }

    public List<Notification> getNotificationsForStudent(Long studentId) {
        return notificationRepository.findByStudent_IdOrderByCreatedAtDesc(studentId);
    }

    public List<Notification> getNotificationsForStudentInClass(Long classId, Long studentId) {
        return notificationRepository.findByClassRoom_IdAndStudent_IdOrderByCreatedAtDesc(classId, studentId);
    }

    @Transactional
    public Notification markRead(Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        if (!n.isReadFlag()) {
            n.setReadFlag(true);
            n.setReadAt(LocalDateTime.now());
            return notificationRepository.save(n);
        }
        return n;
    }
}

