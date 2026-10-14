package io.github.nephets.supplychain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SupplychainApplication {

    private static final Logger log = LoggerFactory.getLogger(SupplychainApplication.class);

    public static void main(String[] args) {
        // 启动入口留痕：默认 H2 内存库 + 默认 JWT 兜底策略都是「本地开发可用、生产必须替换」的，
        // 运维需要能从启动日志里确认这一点。
        log.info("supplychain 启动中，args 数量={}", args.length);
        SpringApplication.run(SupplychainApplication.class, args);
        log.info("supplychain 已就绪（默认 H2 内存库，生产请切换 profile 并注入 SUPPLYCHAIN_JWT_SECRET）");
    }
}
