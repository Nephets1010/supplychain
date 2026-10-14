package io.github.nephets.supplychain.service;

import io.github.nephets.supplychain.domain.AuditLog;
import io.github.nephets.supplychain.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(
            Long actorId,
            String actorName,
            String action,
            String targetType,
            String targetId,
            String detail) {
        // 审计是合规留痕，写失败必须可见：这里不吞异常（由全局异常处理器统一记录），
        // 同时用 debug 留下「谁在什么时候对什么做了什么」的可对照流水。
        log.debug(
                "审计落库 actorId={} actorName={} action={} targetType={} targetId={}",
                actorId,
                actorName,
                action,
                targetType,
                targetId);
        auditLogRepository.save(
                new AuditLog(actorId, actorName, action, targetType, targetId, detail));
    }
}
