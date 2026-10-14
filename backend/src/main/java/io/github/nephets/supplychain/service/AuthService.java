package io.github.nephets.supplychain.service;

import io.github.nephets.supplychain.common.BizException;
import io.github.nephets.supplychain.domain.AppUser;
import io.github.nephets.supplychain.repository.AppUserRepository;
import io.github.nephets.supplychain.security.JwtUtil;
import io.github.nephets.supplychain.security.LoginResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditService auditService;

    public AuthService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            AuditService auditService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.auditService = auditService;
    }

    public LoginResponse login(String username, String password) {
        AppUser user =
                appUserRepository
                        .findByUsername(username)
                        .orElseThrow(() -> new BizException("用户不存在"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BizException("密码错误");
        }
        String token = jwtUtil.generate(user.getUsername(), user.getRole());
        auditService.record(
                user.getId(),
                user.getUsername(),
                "LOGIN",
                "USER",
                String.valueOf(user.getId()),
                "{}");
        log.info("登录成功: username={}, role={}", user.getUsername(), user.getRole());
        return new LoginResponse(token, user.getRole(), user.getUsername());
    }

    public AppUser me(String username) {
        return appUserRepository
                .findByUsername(username)
                .orElseThrow(() -> new BizException("用户不存在"));
    }
}
