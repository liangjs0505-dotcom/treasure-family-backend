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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String GENERIC_ERROR = "用户名或密码错误";

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthProperties properties;
    private final TransactionTemplate failureTx;
    private final String dummyHash;

    public AuthService(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthProperties properties,
            PlatformTransactionManager transactionManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.properties = properties;
        this.failureTx = new TransactionTemplate(transactionManager);
        this.failureTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
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
    public LoginResponse register(String rawUsername, String rawPassword, String rawAdminPassword) {
        String username = normalizeUsername(rawUsername);
        if (username.equals(normalizeUsername(properties.getAdminUsername()))
                || userRepository.findByUsernameIgnoreCase(username).isPresent()) {
            throw new BusinessException(HttpStatus.CONFLICT, "用户名已被占用");
        }
        if (rawAdminPassword == null || rawAdminPassword.isBlank()) {
            throw new BusinessException("管理员密码不能为空");
        }
        if (rawAdminPassword.length() < 5 || rawAdminPassword.length() > 8) {
            throw new BusinessException("管理员密码须为 5 到 8 位");
        }
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setAdminPasswordHash(passwordEncoder.encode(rawAdminPassword));
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
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, lockMessage(user.getLockedUntil()));
        }
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            Failure failure = registerFailure(user.getId());
            if (failure.locked()) {
                throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, lockMessage(failure.lockedUntil()));
            }
            int left = properties.getMaxFailedAttempts() - failure.failedAttempts();
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户名或密码错误，还可以再试 " + left + " 次");
        }
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        String token = jwtService.createToken(user.getId(), user.getUsername());
        return new LoginResponse(token, "Bearer", jwtService.expiresInSeconds(), user.getUsername());
    }

    public void requireAdminPassword(String password) {
        AppUser user = currentUser();
        if (!adminPasswordMatches(user, password)) {
            passwordEncoder.matches(password, dummyHash);
            throw new BusinessException("管理员密码不正确");
        }
    }

    @Transactional
    public void resetAdminPassword(String currentPassword, String newPassword) {
        if (newPassword == null || newPassword.isBlank() || newPassword.length() < 5 || newPassword.length() > 8) {
            throw new BusinessException("管理员密码须为 5 到 8 位");
        }
        AppUser user = currentUser();
        if (!adminPasswordMatches(user, currentPassword)) {
            passwordEncoder.matches(currentPassword, dummyHash);
            throw new BusinessException("原密码不正确");
        }
        user.setAdminPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private AppUser currentUser() {
        return userRepository.findById(CurrentUser.require().userId())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "未登录"));
    }

    private boolean adminPasswordMatches(AppUser user, String password) {
        String hash = user.getAdminPasswordHash();
        if (hash == null || hash.isBlank()) {
            return "admin".equals(password);
        }
        return passwordEncoder.matches(password, hash);
    }

    private Failure registerFailure(String userId) {
        Failure failure = failureTx.execute(status -> {
            AppUser current = userRepository.findById(userId).orElseThrow();
            int next = current.getFailedAttempts() + 1;
            current.setFailedAttempts(next);
            if (next >= properties.getMaxFailedAttempts()) {
                current.setLockedUntil(Instant.now().plus(Duration.ofMinutes(properties.getLockMinutes())));
                current.setFailedAttempts(0);
            }
            userRepository.saveAndFlush(current);
            return new Failure(current.getFailedAttempts(), current.getLockedUntil());
        });
        if (failure == null) {
            throw new IllegalStateException("无法记录登录失败");
        }
        return failure;
    }

    private record Failure(int failedAttempts, Instant lockedUntil) {
        boolean locked() {
            return lockedUntil != null && Instant.now().isBefore(lockedUntil);
        }
    }

    private String lockMessage(Instant until) {
        long seconds = Duration.between(Instant.now(), until).getSeconds();
        long minutes = Math.max(1, (seconds + 59) / 60);
        return "尝试次数过多，请 " + minutes + " 分钟后再试";
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }
}
