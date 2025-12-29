package com.carrot.app.global.security.refreshToken;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
    Optional<RefreshToken> findByRefreshToken(String refreshToken);

    List<RefreshToken> findAllByUserId(Long userId);

    void deleteByRefreshToken(String refreshToken);
}
