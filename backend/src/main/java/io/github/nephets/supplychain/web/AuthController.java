package io.github.nephets.supplychain.web;

import io.github.nephets.supplychain.config.SecurityUtil;
import io.github.nephets.supplychain.domain.AppUser;
import io.github.nephets.supplychain.security.LoginRequest;
import io.github.nephets.supplychain.security.LoginResponse;
import io.github.nephets.supplychain.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final SecurityUtil securityUtil;

    public AuthController(AuthService authService, SecurityUtil securityUtil) {
        this.authService = authService;
        this.securityUtil = securityUtil;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        log.info("登录请求: username={}", request.getUsername());
        return authService.login(request.getUsername(), request.getPassword());
    }

    @GetMapping("/me")
    public AppUser me() {
        AppUser user = securityUtil.current();
        log.info("查询当前用户: username={}", user.getUsername());
        return user;
    }
}
