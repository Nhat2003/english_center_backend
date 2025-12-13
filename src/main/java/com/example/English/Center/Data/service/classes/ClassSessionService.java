package com.example.English.Center.Data.service.classes;

import com.example.English.Center.Data.dto.classes.RescheduleRequest;
import com.example.English.Center.Data.entity.classes.ClassRoom;
import com.example.English.Center.Data.entity.classes.Schedule;
import com.example.English.Center.Data.entity.classes.SessionChangeAudit;
import com.example.English.Center.Data.entity.classes.SessionOverride;
import com.example.English.Center.Data.entity.users.User;
import com.example.English.Center.Data.repository.attendance.AttendanceRepository;
import com.example.English.Center.Data.repository.classes.ScheduleRepository;
import com.example.English.Center.Data.repository.classes.SessionChangeAuditRepository;
import com.example.English.Center.Data.repository.classes.SessionOverrideRepository;
import com.example.English.Center.Data.repository.classes.ClassEntityRepository;
import com.example.English.Center.Data.repository.users.UserRepository;
import com.example.English.Center.Data.entity.announcement.Announcement;
import com.example.English.Center.Data.service.announcement.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

@Service
public class ClassSessionService {

    @Autowired
    private ClassEntityRepository classEntityRepository;

    @Autowired
    private SessionOverrideRepository sessionOverrideRepository;

    @Autowired
    private SessionChangeAuditRepository sessionChangeAuditRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private AnnouncementService announcementService;

    @Transactional
    public SessionOverride rescheduleSession(Long classId, LocalDate originalDate, RescheduleRequest req) {
        // Load class
        ClassRoom classRoom = classEntityRepository.findById(classId).orElseThrow(() -> new IllegalArgumentException("Class not found"));

        // Security: current user must be the teacher assigned to this class
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) throw new SecurityException("Unauthenticated");
        Optional<User> userOpt = userRepository.findByUsername(auth.getName());
        if (userOpt.isEmpty()) throw new SecurityException("User not found");
        User user = userOpt.get();
        if (classRoom.getTeacher() == null || classRoom.getTeacher().getUser() == null || !classRoom.getTeacher().getUser().getId().equals(user.getId())) {
            throw new SecurityException("Forbidden: not the teacher of this class");
        }

        // Determine newStart/newEnd from either ISO fields or date+time parts
        LocalDateTime newStart = null;
        LocalDateTime newEnd = null;
        // Prefer explicit ISO strings when provided
        if (req.getNewStart() != null && !req.getNewStart().isEmpty() && req.getNewEnd() != null && !req.getNewEnd().isEmpty()) {
            newStart = tryParseLocalDateTimeFlexible(req.getNewStart());
            newEnd = tryParseLocalDateTimeFlexible(req.getNewEnd());
            if (newStart == null || newEnd == null) throw new IllegalArgumentException("Invalid datetime format for newStart/newEnd. Accepts ISO_LOCAL_DATE_TIME or 'yyyy-MM-ddTHH:mm' or separate date+time parts.");
        } else if (req.getNewDate() != null && !req.getNewDate().isEmpty() && req.getNewStartTime() != null && !req.getNewStartTime().isEmpty() && req.getNewEndTime() != null && !req.getNewEndTime().isEmpty()) {
            try {
                LocalDate nd = LocalDate.parse(req.getNewDate());
                LocalTime st = tryParseLocalTimeFlexible(req.getNewStartTime());
                LocalTime et = tryParseLocalTimeFlexible(req.getNewEndTime());
                if (st == null || et == null) throw new DateTimeParseException("Invalid time", req.getNewStartTime() + " / " + req.getNewEndTime(), 0);
                newStart = LocalDateTime.of(nd, st);
                newEnd = LocalDateTime.of(nd, et);
            } catch (DateTimeParseException dte) {
                throw new IllegalArgumentException("Invalid date/time format for newDate/newStartTime/newEndTime. Date should be yyyy-MM-dd and time should be HH:mm[:ss] or 'h:mm a' for AM/PM.");
            }
        } else {
            throw new IllegalArgumentException("Missing newStart/newEnd or newDate + newStartTime + newEndTime in request");
        }

        if (!newStart.isBefore(newEnd)) throw new IllegalArgumentException("newStart must be before newEnd");

        // Check original session existence: ensure originalDate within class start/end
        if (originalDate.isBefore(classRoom.getStartDate()) || originalDate.isAfter(classRoom.getEndDate())) {
            throw new IllegalArgumentException("Original date is outside class date range");
        }

        // Ensure no existing override on that original date
        var existing = sessionOverrideRepository.findByClassRoomAndOriginalDateAndStatus(classRoom, originalDate, SessionOverride.OverrideStatus.ACTIVE);
        if (existing.isPresent()) throw new IllegalStateException("An active override already exists for that date");

        // Create override
        SessionOverride so = SessionOverride.builder()
                .classRoom(classRoom)
                .originalDate(originalDate)
                .originalStart(null)
                .originalEnd(null)
                .newStart(newStart)
                .newEnd(newEnd)
                .status(SessionOverride.OverrideStatus.ACTIVE)
                .reason(req.getReason())
                .createdBy(user)
                .createdAt(LocalDateTime.now())
                .build();
        SessionOverride saved = sessionOverrideRepository.save(so);

        // Update attendance rows for that class/date -> new date
        attendanceRepository.bulkUpdateSessionDate(classId, originalDate, newStart.toLocalDate());

        // Remove any persisted Schedule rows that still reference the originalDate so the "old" session disappears
        List<Schedule> schedulesToCheck = scheduleRepository.findByClassRoomId(classId);
        for (Schedule s : schedulesToCheck) {
            if (s.getStartDateTime() != null && s.getStartDateTime().toLocalDate().equals(originalDate)) {
                scheduleRepository.delete(s);
            }
        }

        // Persist change into Schedule table: create a new schedule row for the moved occurrence
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        ZonedDateTime newStartZ = newStart.atZone(zone);
        ZonedDateTime newEndZ = newEnd.atZone(zone);

        Schedule created = Schedule.builder()
                .name("class-" + classId + "-override-" + System.currentTimeMillis())
                .description(req.getReason())
                .classRoom(classRoom)
                .teacher(classRoom.getTeacher())
                .room(classRoom.getRoom())
                .student(null)
                .startDateTime(newStartZ)
                .endDateTime(newEndZ)
                .title("Buổi (đã đổi) - " + classRoom.getName())
                .build();
        scheduleRepository.save(created);

        // Create audit
        SessionChangeAudit audit = SessionChangeAudit.builder()
                .sessionOverride(saved)
                .classRoom(classRoom)
                .action(SessionChangeAudit.Action.CREATED)
                .changedBy(user)
                .changedAt(LocalDateTime.now())
                .details("Rescheduled from " + originalDate + " to " + newStart + " - " + newEnd + "; reason=" + (req.getReason() == null ? "" : req.getReason()))
                .build();
        sessionChangeAuditRepository.save(audit);

        // Optionally create an announcement and notifications to students
        if (Boolean.TRUE.equals(req.getNotifyStudents())) {
            try {
                String title = "Lịch học đã được thay đổi";
                String timeRange = newStart.toLocalTime().toString() + " - " + newEnd.toLocalTime().toString();
                String content = String.format("Lịch học ngày %s được đổi sang %s %s. Học sinh vui lòng kiểm tra lại.%s",
                        originalDate.toString(), newStart.toLocalDate().toString(), timeRange,
                        (req.getReason() != null && !req.getReason().isEmpty()) ? " Lý do: " + req.getReason() : "");

                Announcement ann = Announcement.builder()
                        .classRoom(classRoom)
                        .createdBy(classRoom.getTeacher())
                        .title(title)
                        .content(content)
                        .createdAt(LocalDateTime.now())
                        .build();

                // Use AnnouncementService to persist and fan-out notifications
                announcementService.createAnnouncementForClass(classId, ann);
            } catch (Exception ex) {
                // Log and swallow: do not prevent reschedule if notification fails
                System.err.println("Failed to create announcement/notifications: " + ex.getMessage());
            }
        }

        return saved;
    }

    // Try parsing LocalTime with several common formats (HH:mm:ss, HH:mm, h:mm a)
    private LocalTime tryParseLocalTimeFlexible(String text) {
        if (text == null) return null;
        String t = text.trim();
        DateTimeParseException last = null;
        try {
            return LocalTime.parse(t); // ISO_LOCAL_TIME accepts HH:mm or HH:mm:ss
        } catch (DateTimeParseException ex) {
            last = ex;
        }
        // try AM/PM format
        try {
            DateTimeFormatter fm = DateTimeFormatter.ofPattern("h:mm a");
            return LocalTime.parse(t.toUpperCase(), fm);
        } catch (DateTimeParseException ex) {
            last = ex;
        }
        // try with seconds explicitly
        try {
            DateTimeFormatter fm = DateTimeFormatter.ofPattern("HH:mm:ss");
            return LocalTime.parse(t, fm);
        } catch (DateTimeParseException ex) {
            last = ex;
        }
        return null;
    }

    // Try parsing LocalDateTime: accept ISO_LOCAL_DATE_TIME, or yyyy-MM-dd'T'HH:mm (no seconds)
    private LocalDateTime tryParseLocalDateTimeFlexible(String text) {
        if (text == null) return null;
        String t = text.trim();
        try {
            return LocalDateTime.parse(t); // ISO_LOCAL_DATE_TIME (requires seconds but may accept)
        } catch (DateTimeParseException ex) {
            // try pattern without seconds
            try {
                DateTimeFormatter fm = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
                return LocalDateTime.parse(t, fm);
            } catch (DateTimeParseException ex2) {
                // try with seconds
                try {
                    DateTimeFormatter fm2 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
                    return LocalDateTime.parse(t, fm2);
                } catch (DateTimeParseException ex3) {
                    return null;
                }
            }
        }
    }
}
