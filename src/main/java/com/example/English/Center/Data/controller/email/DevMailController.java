package com.example.English.Center.Data.controller.email;

import com.example.English.Center.Data.service.email.EmailService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/dev/mail")
@ConditionalOnProperty(name = "app.mail-testing-enabled", havingValue = "true", matchIfMissing = false)
public class DevMailController {

    private final EmailService emailService;

    public DevMailController(EmailService emailService) {
        this.emailService = emailService;
    }

    // POST /dev/mail/test with JSON: { "to": "someone@example.com", "subject": "hi", "text": "hello" }
    @PostMapping("/test")
    public ResponseEntity<?> sendTest(@RequestBody Map<String, String> body) {
        String to = body.get("to");
        String subject = body.getOrDefault("subject", "Test email from English Center");
        String text = body.getOrDefault("text", "This is a test email.");

        if (to == null || to.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "'to' is required"));
        }

        try {
            emailService.sendSimpleMessage(to, subject, text);
            return ResponseEntity.ok(Map.of("status", "sent", "to", to));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "failed to send", "message", ex.getMessage()));
        }
    }
}

