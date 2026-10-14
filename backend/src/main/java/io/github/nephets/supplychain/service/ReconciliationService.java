package io.github.nephets.supplychain.service;

import io.github.nephets.supplychain.domain.PurchaseOrder;
import io.github.nephets.supplychain.domain.Reconciliation;
import io.github.nephets.supplychain.repository.PurchaseOrderRepository;
import io.github.nephets.supplychain.repository.ReconciliationRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final AuditService auditService;

    public ReconciliationService(
            PurchaseOrderRepository purchaseOrderRepository,
            ReconciliationRepository reconciliationRepository,
            AuditService auditService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.auditService = auditService;
    }

    /** 对指定日期（订单产生日）的全部已结算（APPROVED）订单做双方对账，按 (商家,店家) 分组汇总。 幂等：先删除该日期既有记录再重建。 */
    @Transactional
    public List<Reconciliation> runDaily(LocalDate date) {
        log.info("每日对账开始: date={}", date);
        Instant start = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        List<PurchaseOrder> approved =
                purchaseOrderRepository.findByStatusAndCreatedAtBetween("APPROVED", start, end);

        Map<String, List<PurchaseOrder>> groups = new LinkedHashMap<>();
        for (PurchaseOrder o : approved) {
            String key = o.getMerchantId() + ":" + o.getShopkeeperId();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(o);
        }

        List<Reconciliation> result = new ArrayList<>();
        for (Map.Entry<String, List<PurchaseOrder>> entry : groups.entrySet()) {
            String[] ids = entry.getKey().split(":");
            Long merchantId = Long.valueOf(ids[0]);
            Long shopkeeperId = Long.valueOf(ids[1]);

            reconciliationRepository
                    .findByReconcileDateAndMerchantIdAndShopkeeperId(date, merchantId, shopkeeperId)
                    .ifPresent(reconciliationRepository::delete);

            Reconciliation r = new Reconciliation(date, merchantId, shopkeeperId);
            r.setOrderCount(entry.getValue().size());
            BigDecimal total = BigDecimal.ZERO;
            BigDecimal fee = BigDecimal.ZERO;
            BigDecimal cost = BigDecimal.ZERO;
            BigDecimal gain = BigDecimal.ZERO;
            for (PurchaseOrder o : entry.getValue()) {
                total = total.add(o.getTotalAmount());
                fee = fee.add(o.getPlatformFee());
                cost = cost.add(o.getMerchantCost());
                gain = gain.add(o.getShopkeeperGain());
            }
            r.setTotalAmount(total);
            r.setPlatformFee(fee);
            r.setMerchantCost(cost);
            r.setShopkeeperGain(gain);
            r.setGeneratedAt(Instant.now());
            result.add(reconciliationRepository.save(r));
        }

        auditService.record(
                null,
                "SYSTEM",
                "RECONCILE",
                "RECONCILE",
                date.toString(),
                "groups=" + result.size());
        log.info("每日对账完成: date={}, groups={}", date, result.size());
        return result;
    }
}
