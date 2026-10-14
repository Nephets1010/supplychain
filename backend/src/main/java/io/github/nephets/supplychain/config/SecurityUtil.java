package io.github.nephets.supplychain.config;

import io.github.nephets.supplychain.domain.AppUser;
import io.github.nephets.supplychain.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {

    private static final Logger log = LoggerFactory.getLogger(SecurityUtil.class);

    private final AppUserRepository appUserRepository;

    public SecurityUtil(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public AppUser current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String name = auth == null ? null : auth.getName();
        return appUserRepository
                .findByUsername(name)
                .orElseThrow(
                        () -> {
                            // 令牌里的用户名在库里查不到，通常意味着用户被删了或令牌来自别的环境；
                            // 这是权限边界上的异常，必须留痕而不是静默变成 500。
                            log.warn("当前用户不存在：username={}", name);
                            return new IllegalStateException("当前用户不存在: " + name);
                        });
    }
}
