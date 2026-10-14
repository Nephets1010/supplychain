package io.github.nephets.supplychain.job;

import io.github.nephets.supplychain.service.ReconciliationService;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 每日 10:00 对账：对前一日全部已结算订单做双方对账。 */
@Component
public class DailyReconcileJob {

    private static final Logger log = LoggerFactory.getLogger(DailyReconcileJob.class);

    private final ReconciliationService reconciliationService;

    public DailyReconcileJob(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @Scheduled(cron = "0 0 10 * * *")
    public void run() {
        LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        try {
            int groups = reconciliationService.runDaily(yesterday).size();
            log.info("每日对账完成，日期={}，分组数={}", yesterday, groups);
        } catch (RuntimeException e) {
            // 为什么收 RuntimeException 并且不重抛：@Scheduled 方法抛出的异常会一路冒到调度线程，
            // 一次对账失败不该连带影响后续调度与进程；这里记全量堆栈即可，对账本身按 (日期,商家,店家)
            // 幂等，下次执行会补上。收 RuntimeException 而不是 Exception——这个方法不抛受检异常，
            // 写 Exception 只会掩盖「到底预期什么失败」。
            log.error("每日对账失败，日期={}；本次未生成对账记录，下次调度会按幂等键补上", yesterday, e);
        }
    }
}
