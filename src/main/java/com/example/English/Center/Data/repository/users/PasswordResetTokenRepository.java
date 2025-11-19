package com.example.English.Center.Data.repository.users;

import com.example.English.Center.Data.entity.users.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteAllByExpiresAtBefore(LocalDateTime time);
}

