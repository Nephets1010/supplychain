package io.github.nephets.supplychain.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    /** 未显式配置密钥时生成的兜底密钥字节数；48 字节远超 HS256 要求的 32 字节。 */
    private static final int FALLBACK_SECRET_BYTES = 48;

    private final String secret;
    private final long expireMinutes;
    private final SecretKey key;

    public JwtUtil(
            @Value("${supplychain.jwt.secret}") String secret,
            @Value("${supplychain.jwt.expire-minutes:480}") long expireMinutes) {
        this.expireMinutes = expireMinutes;
        if (secret == null || secret.isBlank()) {
            // 为什么不再在 yml 里写死一个兜底密钥：那个字符串会进版本库，等于把签名密钥公开发布，
            // 任何人都能给自己签一个 admin token。这里改成「没配就现生成一个随机密钥」——随机密钥
            // 无法被伪造，代价只是重启后已签发的 token 立刻失效，对本地开发完全可接受。
            this.secret = randomSecret();
            log.error(
                    "supplychain.jwt.secret 未配置，已生成一次性随机密钥；重启后所有已签发 token 立即失效。"
                            + "生产必须显式注入至少 32 字节强密钥（环境变量 SUPPLYCHAIN_JWT_SECRET）");
        } else {
            this.secret = secret;
            log.debug("JWT 密钥已从配置注入，expireMinutes={}", expireMinutes);
        }
        // 密钥在构造期一次算好：既是 fail-fast（弱密钥启动即失败，而不是等到第一个请求才炸），
        // 也顺带去掉了原来「首次调用时惰性初始化 SecretKey」的数据竞争。
        this.key = Keys.hmacShaKeyFor(this.secret.getBytes(StandardCharsets.UTF_8));
    }

    private static String randomSecret() {
        byte[] bytes = new byte[FALLBACK_SECRET_BYTES];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** 当前生效密钥的字符数，仅用于排查弱密钥（HS256 要求 ≥32 字节）。 */
    public int secretLength() {
        return secret.length();
    }

    public String generate(String username, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(expireMinutes))))
                .signWith(key)
                .compact();
    }

    /**
     * 解析令牌。
     *
     * <p>为什么在工具类里也留一条日志：令牌被拒（过期 / 被篡改 / 用别的密钥签的）属于安全事件， 只在过滤器里打 debug 的话，运维看不到有人在拿伪造 token 试探。
     */
    public Claims parse(String token) throws JwtException {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("令牌解析失败，已拒绝：{}", e.getMessage());
            throw e;
        }
    }

    public String username(String token) throws JwtException {
        return parse(token).getSubject();
    }

    public String role(String token) throws JwtException {
        return parse(token).get("role", String.class);
    }
}
