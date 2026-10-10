package com.treasurefamily.auth;

import com.treasurefamily.auth.dto.AdminPasswordRequest;
import com.treasurefamily.auth.dto.LoginRequest;
import com.treasurefamily.auth.dto.LoginResponse;
import com.treasurefamily.auth.dto.MeResponse;
import com.treasurefamily.auth.dto.RegisterRequest;
import com.treasurefamily.auth.dto.ResetAdminPasswordRequest;
import com.treasurefamily.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(
                "注册成功",
                authService.register(request.username(), request.password(), request.adminPassword()));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request.username(), request.password()));
    }

    @PostMapping("/admin-password")
    public ApiResponse<Void> adminPassword(@Valid @RequestBody AdminPasswordRequest request) {
        authService.requireAdminPassword(request.password());
        return ApiResponse.ok(null);
    }

    @PostMapping("/admin-password/reset")
    public ApiResponse<Void> resetAdminPassword(@Valid @RequestBody ResetAdminPasswordRequest request) {
        authService.resetAdminPassword(request.currentPassword(), request.newPassword());
        return ApiResponse.ok("管理员密码已重置", null);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.ok("已退出", null);
    }

    @GetMapping("/me")
    public ApiResponse<MeResponse> me(Authentication authentication) {
        return ApiResponse.ok(new MeResponse(authentication.getName()));
    }
}
