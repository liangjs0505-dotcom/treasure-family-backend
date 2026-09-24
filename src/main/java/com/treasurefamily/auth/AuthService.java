package com.treasurefamily.auth;

import com.treasurefamily.auth.dto.LoginResponse;
import com.treasurefamily.common.BusinessException;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String GENERIC_ERROR = "用户名或密码错误";

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthProperties properties;
    private final String dummyHash;

    public AuthService(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.properties = properties;
        this.dummyHash = passwordEncoder.encode("invalid-password");
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void ensureAdmin() {
        String username = normalizeUsername(properties.getAdminUsername());
        String password = properties.getAdminPassword();
        if (userRepository.findByUsernameIgnoreCase(username).isPresent()) {
            return;
        }
        if (password == null || password.length() < 12) {
            log.warn("未设置 ADMIN_PASSWORD（至少 12 位），管理员账号未创建，将无法登录");
            return;
        }
        AppUser admin = new AppUser();
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setEnabled(true);
        userRepository.save(admin);
        log.info("已创建管理员账号 {}", username);
    }

    @Transactional
    public LoginResponse register(String rawUsername, String rawPassword) {
        String username = normalizeUsername(rawUsername);
        if (username.equals(normalizeUsername(properties.getAdminUsername()))
                || userRepository.findByUsernameIgnoreCase(username).isPresent()) {
            throw new BusinessException(HttpStatus.CONFLICT, "用户名已被占用");
        }
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setEnabled(true);
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(HttpStatus.CONFLICT, "用户名已被占用");
        }
        String token = jwtService.createToken(user.getId(), user.getUsername());
        return new LoginResponse(token, "Bearer", jwtService.expiresInSeconds(), user.getUsername());
    }

    @Transactional
    public LoginResponse login(String rawUsername, String rawPassword) {
        String username = normalizeUsername(rawUsername);
        AppUser user = userRepository.findByUsernameIgnoreCase(username).orElse(null);
        if (user == null) {
            passwordEncoder.matches(rawPassword, dummyHash);
            throw new BusinessException(HttpStatus.UNAUTHORIZED, GENERIC_ERROR);
        }
        if (!user.isEnabled()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, GENERIC_ERROR);
        }
        if (user.isLocked()) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "尝试次数过多，请稍后再试");
        }
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            registerFailure(user);
            throw new BusinessException(HttpStatus.UNAUTHORIZED, GENERIC_ERROR);
        }
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        String token = jwtService.createToken(user.getId(), user.getUsername());
        return new LoginResponse(token, "Bearer", jwtService.expiresInSeconds(), user.getUsername());
    }

    private void registerFailure(AppUser user) {
        int next = user.getFailedAttempts() + 1;
        user.setFailedAttempts(next);
        if (next >= properties.getMaxFailedAttempts()) {
            user.setLockedUntil(Instant.now().plus(Duration.ofMinutes(properties.getLockMinutes())));
            user.setFailedAttempts(0);
        }
        userRepository.save(user);
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }
}
