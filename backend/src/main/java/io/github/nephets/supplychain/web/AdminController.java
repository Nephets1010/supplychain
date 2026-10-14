package io.github.nephets.supplychain.web;

import io.github.nephets.supplychain.config.SecurityUtil;
import io.github.nephets.supplychain.domain.AppUser;
import io.github.nephets.supplychain.domain.AuditLog;
import io.github.nephets.supplychain.domain.PurchaseOrder;
import io.github.nephets.supplychain.domain.Reconciliation;
import io.github.nephets.supplychain.repository.AuditLogRepository;
import io.github.nephets.supplychain.repository.PurchaseOrderRepository;
import io.github.nephets.supplychain.repository.ReconciliationRepository;
import io.github.nephets.supplychain.service.OrderService;
import io.github.nephets.supplychain.service.ReconciliationService;
import io.github.nephets.supplychain.web.dto.OrderDetailResponse;
import io.github.nephets.supplychain.web.dto.ReviewRequest;
import io.github.nephets.supplychain.web.dto.StatsResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final AuditLogRepository auditLogRepository;
    private final OrderService orderService;
    private final ReconciliationService reconciliationService;
    private final SecurityUtil securityUtil;

    public AdminController(
            PurchaseOrderRepository purchaseOrderRepository,
            ReconciliationRepository reconciliationRepository,
            AuditLogRepository auditLogRepository,
            OrderService orderService,
            ReconciliationService reconciliationService,
            SecurityUtil securityUtil) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.auditLogRepository = auditLogRepository;
        this.orderService = orderService;
        this.reconciliationService = reconciliationService;
        this.securityUtil = securityUtil;
    }

    @GetMapping("/orders")
    public List<PurchaseOrder> orders(@RequestParam(required = false) String status) {
        log.info("管理方查询订单列表: status={}", status);
        if (status == null || status.isBlank()) {
            return purchaseOrderRepository.findAll();
        }
        return purchaseOrderRepository.findByStatus(status);
    }

    @GetMapping("/orders/{id}")
    public OrderDetailResponse orderDetail(@PathVariable Long id) {
        log.info("管理方查询订单详情: id={}", id);
        return orderService.detail(id);
    }

    @PostMapping("/orders/{id}/review")
    public PurchaseOrder review(@PathVariable Long id, @RequestBody ReviewRequest request) {
        AppUser admin = securityUtil.current();
        log.info(
                "管理方审核订单: id={}, status={}, reviewer={}",
                id,
                request.getStatus(),
                admin.getUsername());
        return orderService.review(
                id,
                request.getStatus(),
                request.getItems(),
                admin.getId(),
                admin.getUsername(),
                request.getNote());
    }

    @GetMapping("/reconciliations")
    public List<Reconciliation> reconciliations(@RequestParam(required = false) String date) {
        log.info("管理方查询对账记录: date={}", date);
        if (date == null || date.isBlank()) {
            return reconciliationRepository.findAll();
        }
        return reconciliationRepository.findByReconcileDate(LocalDate.parse(date));
    }

    @PostMapping("/reconciliations/run")
    public List<Reconciliation> runReconcile(@RequestParam(required = false) String date) {
        LocalDate target =
                (date == null || date.isBlank())
                        ? LocalDate.now(ZoneOffset.UTC).minusDays(1)
                        : LocalDate.parse(date);
        log.info("管理方触发对账: date={}", target);
        return reconciliationService.runDaily(target);
    }

    @GetMapping("/audit")
    public List<AuditLog> audit() {
        log.info("管理方查询审计日志");
        return auditLogRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @GetMapping("/stats")
    public StatsResponse stats() {
        log.info("管理方查询统计概览");
        List<PurchaseOrder> all = purchaseOrderRepository.findAll();
        int total = all.size();
        int pending = 0;
        int approved = 0;
        int rejected = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalPlatformFee = BigDecimal.ZERO;
        for (PurchaseOrder o : all) {
            if ("PENDING".equals(o.getStatus())) {
                pending++;
            } else if ("APPROVED".equals(o.getStatus())) {
                approved++;
                totalAmount = totalAmount.add(o.getTotalAmount());
                totalPlatformFee = totalPlatformFee.add(o.getPlatformFee());
            } else if ("REJECTED".equals(o.getStatus())) {
                rejected++;
            }
        }
        return new StatsResponse(total, pending, approved, rejected, totalAmount, totalPlatformFee);
    }
}
