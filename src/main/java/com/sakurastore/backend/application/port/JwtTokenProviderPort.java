package com.sakurastore.backend.application.port;

import com.sakurastore.backend.domain.model.User;

public interface JwtTokenProviderPort {
    String generateToken(User user);
    String getUsernameFromToken(String token);
    boolean validateToken(String token);
}
