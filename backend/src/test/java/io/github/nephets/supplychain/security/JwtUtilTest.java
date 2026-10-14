package io.github.nephets.supplychain.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

/**
 * JWT 密钥兜底策略回归测试。
 *
 * <p>背景：以前 yml 里写死了一个兜底密钥（{@code supplychain.jwt.secret-...}），等于把签名密钥发布在版本库里， 任何人都能给自己签一个 admin
 * token。现在改成「未注入就用一次性随机密钥」，这里锁住三条性质： 没配也能正常签发与解析、两个实例不共享密钥（证明没有任何写死的默认值）、配了就用配置值。
 */
class JwtUtilTest {

    /** 48 个字符，超过 HS256 要求的 32 字节。 */
    private static final String CONFIGURED_SECRET =
            "unit-test-configured-secret-0123456789abcdefghij";

    @Test
    void blank_secret_falls_back_to_random_key_and_still_roundtrips() {
        JwtUtil util = new JwtUtil("", 480);
        String token = util.generate("shop1", "SHOPKEEPER");
        assertThat(util.username(token)).isEqualTo("shop1");
        assertThat(util.role(token)).isEqualTo("SHOPKEEPER");
    }

    @Test
    void null_secret_also_falls_back() {
        JwtUtil util = new JwtUtil(null, 480);
        assertThat(util.username(util.generate("admin", "ADMIN"))).isEqualTo("admin");
    }

    @Test
    void two_instances_without_secret_do_not_share_a_key() {
        JwtUtil a = new JwtUtil("", 480);
        JwtUtil b = new JwtUtil("", 480);

        // 关键断言：如果 yml 或代码里还存在任何写死的兜底密钥，b 就能验过 a 签的 token。
        assertThatThrownBy(() -> b.parse(a.generate("admin", "ADMIN")))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void configured_secret_is_used_and_roundtrips() {
        JwtUtil util = new JwtUtil(CONFIGURED_SECRET, 480);
        String token = util.generate("merchant1", "MERCHANT");
        assertThat(util.username(token)).isEqualTo("merchant1");
        assertThat(util.role(token)).isEqualTo("MERCHANT");
        assertThat(util.secretLength()).isEqualTo(CONFIGURED_SECRET.length());
    }

    @Test
    void token_signed_with_another_secret_is_rejected() {
        JwtUtil util = new JwtUtil(CONFIGURED_SECRET, 480);
        JwtUtil other = new JwtUtil(CONFIGURED_SECRET + "-different-suffix", 480);
        assertThatThrownBy(() -> util.parse(other.generate("admin", "ADMIN")))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void garbage_token_is_rejected() {
        JwtUtil util = new JwtUtil(CONFIGURED_SECRET, 480);
        assertThatThrownBy(() -> util.parse("not-a-jwt")).isInstanceOf(JwtException.class);
    }

    @Test
    void weak_key_fails_fast_at_construction() {
        // 少于 32 字节的密钥不可能安全：必须在构造期就炸，而不是等到第一个请求。
        // 具体异常类型由 jjwt 定义（WeakKeyException），这里只锁定「构造期抛运行时异常」这条性质。
        assertThatThrownBy(() -> new JwtUtil("too-short", 480))
                .isInstanceOf(RuntimeException.class);
    }
}
