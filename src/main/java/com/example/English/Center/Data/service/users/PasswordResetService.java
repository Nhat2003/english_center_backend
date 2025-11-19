package com.example.English.Center.Data.service.users;

import com.example.English.Center.Data.entity.users.PasswordResetToken;
import com.example.English.Center.Data.entity.users.User;
import com.example.English.Center.Data.repository.students.StudentRepository;
import com.example.English.Center.Data.repository.teachers.TeacherRepository;
import com.example.English.Center.Data.repository.users.PasswordResetTokenRepository;
import com.example.English.Center.Data.repository.users.UserRepository;
import com.example.English.Center.Data.service.email.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    private final int EXPIRY_MINUTES = 15;

    @Value("${app.frontend.reset-url:http://localhost:4200/reset-password}")
    private String frontendResetUrl;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                                UserRepository userRepository,
                                StudentRepository studentRepository,
                                TeacherRepository teacherRepository,
                                EmailService emailService,
                                PasswordEncoder passwordEncoder) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    // Inner class to hold resolved user + email destination
    private static class ResolvedUser {
        User user;
        String emailDestination;
        ResolvedUser(User user, String emailDestination) {
            this.user = user;
            this.emailDestination = emailDestination;
        }
    }

    private Optional<ResolvedUser> resolveUserWithEmail(String identifier) {
        if (identifier == null) return Optional.empty();
        identifier = identifier.trim();
        if (identifier.isEmpty()) return Optional.empty();
        // 1) try username directly
        Optional<User> u = userRepository.findByUsername(identifier);
        if (u.isPresent()) {
            logger.debug("resolveUser: matched username='{}' -> userId={}", identifier, u.get().getId());
            return Optional.of(new ResolvedUser(u.get(), identifier)); // use identifier as email (may or may not work)
        }

        // 1b) try username ignore case
        Optional<User> u2 = userRepository.findByUsernameIgnoreCase(identifier);
        if (u2.isPresent()) {
            logger.debug("resolveUser: matched usernameIgnoreCase='{}' -> userId={}", identifier, u2.get().getId());
            return Optional.of(new ResolvedUser(u2.get(), identifier));
        }

        // 2) try student's email
        var sOpt = studentRepository.findByEmailIgnoreCase(identifier);
        if (sOpt.isPresent()) {
            var s = sOpt.get();
            if (s.getUser() != null) {
                logger.debug("resolveUser: matched student.email='{}' -> userId={}", identifier, s.getUser().getId());
                return Optional.of(new ResolvedUser(s.getUser(), s.getEmail())); // use student's email
            } else {
                logger.warn("resolveUser: found student record with email='{}' but user link is null (student.id={})", identifier, s.getId());
            }
        }

        // 3) try teacher's email
        var tOpt = teacherRepository.findByEmailIgnoreCase(identifier);
        if (tOpt.isPresent()) {
            var t = tOpt.get();
            if (t.getUser() != null) {
                logger.debug("resolveUser: matched teacher.email='{}' -> userId={}", identifier, t.getUser().getId());
                return Optional.of(new ResolvedUser(t.getUser(), t.getEmail())); // use teacher's email
            } else {
                logger.warn("resolveUser: found teacher record with email='{}' but user link is null (teacher.id={})", identifier, t.getId());
            }
        }

        logger.debug("resolveUser: no user found for identifier='{}'", identifier);
        return Optional.empty();
    }

    public String createTokenAndSendEmail(String email) {
        Optional<ResolvedUser> resolvedOpt = resolveUserWithEmail(email);
        if (resolvedOpt.isEmpty()) {
            throw new IllegalArgumentException("Email does not exist");
        }

        ResolvedUser resolved = resolvedOpt.get();
        User user = resolved.user;
        String token = UUID.randomUUID().toString();

        PasswordResetToken prt = PasswordResetToken.builder()
                .userId(user.getId())
                .token(token)
                .expiresAt(LocalDateTime.now().plusMinutes(EXPIRY_MINUTES))
                .build();

        tokenRepository.save(prt);

        String resetLink = frontendResetUrl + "?token=" + token;
        String subject = "Reset mật khẩu - TOEIC DOWNTOWN";
        String text = "Chào " + user.getFullName() + ",\n\n" +
                "Bạn (hoặc người dùng khác) đã yêu cầu đặt lại mật khẩu. Vui lòng bấm vào đường dẫn bên dưới để đặt lại mật khẩu (hết hạn sau " + EXPIRY_MINUTES + " phút):\n\n" +
                resetLink + "\n\n" +
                "Nếu bạn không yêu cầu, vui lòng bỏ qua email này.";

        try {
            emailService.sendSimpleMessage(resolved.emailDestination, subject, text);
            logger.info("Sent reset link email to '{}' for userId={}", resolved.emailDestination, user.getId());
        } catch (Exception ex) {
            logger.error("Failed to send reset email to '{}'; token={} userId={}", resolved.emailDestination, token, user.getId(), ex);
        }

        logger.info("Password reset token created: token={} for userId={} (expires in {} minutes)", token, user.getId(), EXPIRY_MINUTES);

        return token;
    }

    public void resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> prtOpt = tokenRepository.findByToken(token);
        if (prtOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid token");
        }
        PasswordResetToken prt = prtOpt.get();
        if (prt.getExpiresAt().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(prt);
            throw new IllegalArgumentException("Token expired");
        }

        Optional<User> userOpt = userRepository.findById(prt.getUserId());
        if (userOpt.isEmpty()) {
            tokenRepository.delete(prt);
            throw new IllegalArgumentException("User not found");
        }
        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        tokenRepository.delete(prt);
    }

    public String createAndSendNewPassword(String email) {
        Optional<ResolvedUser> resolvedOpt = resolveUserWithEmail(email);
        if (resolvedOpt.isEmpty()) {
            throw new IllegalArgumentException("Email does not exist");
        }
        ResolvedUser resolved = resolvedOpt.get();
        User user = resolved.user;

        String newPassword = generateRandomPassword(12);
        String encoded = passwordEncoder.encode(newPassword);
        user.setPassword(encoded);
        userRepository.save(user);

        String subject = "Mật khẩu mới - TOEIC DOWNTOWN";
        String text = "Chào " + user.getFullName() + ",\n\n" +
                "Hệ thống đã tạo một mật khẩu mới cho tài khoản của bạn. Vui lòng đăng nhập bằng mật khẩu tạm thời sau và đổi mật khẩu ngay khi đăng nhập:\n\n" +
                "Mật khẩu tạm thời: " + newPassword + "\n\n" +
                "Lưu ý: Mật khẩu này có thể sử dụng ngay. Nếu bạn không yêu cầu, vui lòng liên hệ admin.";

        try {
            emailService.sendSimpleMessage(resolved.emailDestination, subject, text);
            logger.info("Sent temporary password to '{}' for userId={}", resolved.emailDestination, user.getId());
        } catch (Exception ex) {
            logger.error("Failed to send temporary password to '{}' for userId={}", resolved.emailDestination, user.getId(), ex);
        }

        logger.info("Temporary password created for userId={}", user.getId());
        return newPassword;
    }

    private String generateRandomPassword(int length) {
        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%&*";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int idx = random.nextInt(chars.length());
            sb.append(chars.charAt(idx));
        }
        return sb.toString();
    }
}
