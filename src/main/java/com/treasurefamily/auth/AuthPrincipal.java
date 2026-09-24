package com.treasurefamily.auth;

import org.springframework.security.core.AuthenticatedPrincipal;

public record AuthPrincipal(String userId, String username) implements AuthenticatedPrincipal {

    @Override
    public String getName() {
        return username;
    }
}
