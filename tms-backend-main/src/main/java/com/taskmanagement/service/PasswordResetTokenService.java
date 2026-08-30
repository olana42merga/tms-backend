package com.taskmanagement.service;

import com.taskmanagement.entity.PasswordResetToken;
import com.taskmanagement.entity.User;
import com.taskmanagement.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetTokenService {

    private final PasswordResetTokenRepository tokenRepository;

    private static final int EXPIRATION_HOURS = 24;

    @Transactional
    public void createToken(User user, String token) {
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusHours(EXPIRATION_HOURS));
        tokenRepository.save(resetToken);
        log.info("✅ Password reset token created for user: {}", user.getUsername());
    }

    public boolean isValidToken(String token) {
        return tokenRepository.findByToken(token)
                .map(t -> !t.isExpired())
                .orElse(false);
    }

    public User getUserByToken(String token) {
        return tokenRepository.findByToken(token)
                .map(PasswordResetToken::getUser)
                .orElse(null);
    }

    @Transactional
    public void deleteToken(String token) {
        tokenRepository.deleteByToken(token);
        log.info("✅ Password reset token deleted: {}", token);
    }
}
