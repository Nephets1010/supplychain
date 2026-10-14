package io.github.nephets.supplychain.web;

import io.github.nephets.supplychain.service.SettlementService;
import io.github.nephets.supplychain.web.dto.SettleItem;
import io.github.nephets.supplychain.web.dto.SettlementResult;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settle")
public class SettleController {

    private static final Logger log = LoggerFactory.getLogger(SettleController.class);

    private final SettlementService settlementService;

    public SettleController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping("/preview")
    public SettlementResult preview(@RequestBody List<SettleItem> items) {
        int size = items == null ? 0 : items.size();
        log.info("分账预览: itemCount={}", size);
        return settlementService.settle(items);
    }
}
