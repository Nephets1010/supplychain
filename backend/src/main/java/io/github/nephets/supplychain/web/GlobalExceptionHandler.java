package io.github.nephets.supplychain.web;

import io.github.nephets.supplychain.common.BizException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Map<String, String>> biz(BizException e) {
        log.warn("业务异常被拒绝: {}", e.getMessage());
        return ResponseEntity.status(400)
                .body(Map.of("error", "BAD_REQUEST", "message", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> illegal(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        return ResponseEntity.status(400)
                .body(Map.of("error", "BAD_REQUEST", "message", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> any(Exception e) {
        log.error("未处理异常: {}", e.getMessage(), e);
        return ResponseEntity.status(500)
                .body(Map.of("error", "INTERNAL_ERROR", "message", e.getMessage()));
    }
}
