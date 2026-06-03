package com.nexuserp.sales.adapter.in.rest;

import com.nexuserp.sales.domain.model.SalesOrder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SalesOrderDto(
    UUID id, String tenantId, String orderNumber,
    UUID customerId, String customerName, String customerRef,
    String status, LocalDate orderDate, LocalDate requestedDeliveryDate,
    String currency, BigDecimal totalAmount,
    String shippingAddress, String notes
) {
    public static SalesOrderDto from(SalesOrder o) {
        return new SalesOrderDto(
            o.getId(), o.getTenantId().value(), o.getOrderNumber(),
            o.getCustomerId(), o.getCustomerName(), o.getCustomerRef(),
            o.getStatus().name(), o.getOrderDate(), o.getRequestedDeliveryDate(),
            o.getCurrency(), o.getTotal() != null ? o.getTotal().amount() : null,
            o.getShippingAddress(), o.getNotes()
        );
    }
}
