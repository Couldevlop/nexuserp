package com.nexuserp.inventory.domain.port.out;

import com.nexuserp.inventory.domain.event.StockLowAlertEvent;

public interface InventoryEventPublisher {
    void publishLowStockAlert(StockLowAlertEvent event);
}
