package com.treasurefamily.auth;

import com.treasurefamily.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static AuthPrincipal require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        if (principal.userId() == null || principal.userId().isBlank()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        return principal;
    }
}
