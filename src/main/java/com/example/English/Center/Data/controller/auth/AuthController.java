package com.example.English.Center.Data.controller.auth;

import com.example.English.Center.Data.dto.auth.ResetPasswordRequest;
import com.example.English.Center.Data.service.users.PasswordResetService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@Validated
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final PasswordResetService passwordResetService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    /**
     * New behavior: if email exists, generate a new password, save it and send it to the user's email.
     * For development we also return the plaintext new password in the response.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @RequestParam(value = "email", required = false) String emailParam,
            HttpServletRequest request) {

        String email = emailParam;

        // If no query/form param, try to read JSON body safely
        if (email == null || email.isBlank()) {
            try {
                StringBuilder sb = new StringBuilder();
                BufferedReader reader = request.getReader();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                String body = sb.toString().trim();
                if (!body.isEmpty()) {
                    Map<String, String> parsed = objectMapper.readValue(body, new TypeReference<>() {});
                    if (parsed != null) {
                        email = parsed.get("email");
                    }
                } else {
                    // also check classic form param (application/x-www-form-urlencoded)
                    String formEmail = request.getParameter("email");
                    if (formEmail != null && !formEmail.isBlank()) {
                        email = formEmail;
                    }
                }
            } catch (Exception ex) {
                logger.debug("Could not read request body for forgot-password: {}", ex.getMessage());
            }
        }

        if (email == null || email.isBlank()) {
            logger.debug("Forgot-password called without email param or body");
            return ResponseEntity.badRequest().body(Map.of("message", "email required"));
        }

        try {
            String newPassword = passwordResetService.createAndSendNewPassword(email);
            return ResponseEntity.ok(Map.of(
                    "message", "Temporary password generated and emailed",
                    "temporaryPassword", newPassword, // dev only
                    "devNote", "Temporary password is returned in response for testing; remove in production"
            ));
        } catch (IllegalArgumentException e) {
            logger.debug("Forgot-password error for email={}: {}", email, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException ise) {
            // Email sending failed - password was NOT changed in database
            logger.error("Forgot-password: failed to send email for email={}: {}", email, ise.getMessage());
            return ResponseEntity.status(502).body(Map.of(
                    "message", "Failed to send email. Please try again later or contact support.",
                    "error", "EMAIL_SEND_FAILED"
            ));
        } catch (Exception ex) {
            logger.error("Unexpected error in forgotPassword for email={}", email, ex);
            return ResponseEntity.status(500).body(Map.of("message", "Internal server error"));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        try {
            passwordResetService.resetPassword(req.getToken(), req.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Password updated"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
