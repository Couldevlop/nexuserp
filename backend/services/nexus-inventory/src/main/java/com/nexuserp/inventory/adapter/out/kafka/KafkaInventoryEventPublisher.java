package com.nexuserp.inventory.adapter.out.kafka;

import com.nexuserp.inventory.domain.event.StockLowAlertEvent;
import com.nexuserp.inventory.domain.port.out.InventoryEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaInventoryEventPublisher implements InventoryEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaInventoryEventPublisher.class);
    private static final String TOPIC_STOCK_ALERT = "nexus.inventory.stock-alert";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaInventoryEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishLowStockAlert(StockLowAlertEvent event) {
        kafkaTemplate.send(TOPIC_STOCK_ALERT, event.tenantId(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish low stock alert for product={}", event.productCode(), ex);
                } else {
                    log.info("Low stock alert published: product={}, qty={}",
                        event.productCode(), event.currentQuantity());
                }
            });
    }
}
