package com.yaqazah.user.repository;

import com.yaqazah.user.model.AuthToken;
import com.yaqazah.user.model.TokenType;
import com.yaqazah.user.model.User;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

@NullMarked
public interface AuthTokenRepository extends JpaRepository<AuthToken, UUID> {
    Optional<AuthToken> findByTokenAndUserAndTokenType(String token, User user, TokenType type);

    Optional<AuthToken> findByTokenAndTokenType(String token, TokenType type);

    void deleteByUserAndTokenType(User user, TokenType type);
}

