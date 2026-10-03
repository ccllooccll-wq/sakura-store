package com.sakurastore.backend.infrastructure.security;

import java.io.Serializable;

public record SessionIdentity(Long id, String username, String passwordHash)
    implements Serializable {}
