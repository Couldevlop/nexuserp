package com.nexuserp.procurement.domain.model;

import com.nexuserp.core.domain.value.Money;
import com.nexuserp.core.domain.event.DomainEvent;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * PurchaseOrder aggregate — covers the full procurement workflow:
 * PurchaseRequest → RFQ → Quote → PurchaseOrder → GoodsReceipt → SupplierInvoice → Payment
 */
@Getter
public class PurchaseOrder {

    public enum Status {
        DRAFT, SUBMITTED, APPROVED, SENT_TO_SUPPLIER,
        PARTIALLY_RECEIVED, RECEIVED, INVOICED, CLOSED, CANCELLED
    }

    private final String id;
    private final String tenantId;
    private final String orderNumber;
    private final String supplierId;
    private final String supplierName;
    private final String currency;
    private Status status;
    private final LocalDate orderDate;
    private LocalDate expectedDeliveryDate;
    private LocalDate actualDeliveryDate;
    private final List<PurchaseOrderLine> lines;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private String notes;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    @Builder
    private PurchaseOrder(String id, String tenantId, String orderNumber,
                          String supplierId, String supplierName, String currency,
                          LocalDate orderDate, LocalDate expectedDeliveryDate,
                          String notes, List<PurchaseOrderLine> lines) {
        this.id = Objects.requireNonNull(id);
        this.tenantId = Objects.requireNonNull(tenantId);
        this.orderNumber = Objects.requireNonNull(orderNumber);
        this.supplierId = Objects.requireNonNull(supplierId);
        this.supplierName = Objects.requireNonNull(supplierName);
        this.currency = Objects.requireNonNull(currency);
        this.orderDate = Objects.requireNonNull(orderDate);
        this.expectedDeliveryDate = expectedDeliveryDate;
        this.notes = notes;
        this.status = Status.DRAFT;
        this.lines = new ArrayList<>(Objects.requireNonNull(lines));
        recalculate();
    }

    public void submit(String userId) {
        assertStatus(Status.DRAFT, "submit");
        if (lines.isEmpty()) {
            throw new IllegalStateException("Cannot submit a purchase order with no lines");
        }
        this.status = Status.SUBMITTED;
    }

    public void approve(String approverId) {
        assertStatus(Status.SUBMITTED, "approve");
        this.approvedBy = approverId;
        this.approvedAt = LocalDateTime.now();
        this.status = Status.APPROVED;
    }

    public void sendToSupplier(String userId) {
        assertStatus(Status.APPROVED, "send to supplier");
        this.status = Status.SENT_TO_SUPPLIER;
    }

    public void recordPartialReceipt(String userId) {
        if (status != Status.SENT_TO_SUPPLIER && status != Status.PARTIALLY_RECEIVED) {
            throw new IllegalStateException("Cannot record receipt for status: " + status);
        }
        this.status = Status.PARTIALLY_RECEIVED;
    }

    public void recordFullReceipt(LocalDate deliveryDate, String userId) {
        if (status != Status.SENT_TO_SUPPLIER && status != Status.PARTIALLY_RECEIVED) {
            throw new IllegalStateException("Cannot mark as received for status: " + status);
        }
        this.actualDeliveryDate = deliveryDate;
        this.status = Status.RECEIVED;
    }

    public void cancel(String userId, String reason) {
        if (status == Status.INVOICED || status == Status.CLOSED) {
            throw new IllegalStateException("Cannot cancel a " + status + " purchase order");
        }
        this.status = Status.CANCELLED;
    }

    public Money getSubtotalAmount() {
        return lines.stream()
                .map(PurchaseOrderLine::getSubtotal)
                .reduce(Money.zero(currency), Money::add);
    }

    public Money getTotalTaxAmount() {
        return lines.stream()
                .map(PurchaseOrderLine::getTaxAmount)
                .reduce(Money.zero(currency), Money::add);
    }

    public Money getTotalAmount() {
        return getSubtotalAmount().add(getTotalTaxAmount());
    }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    private void recalculate() {
        lines.forEach(PurchaseOrderLine::recalculate);
    }

    private void assertStatus(Status expected, String action) {
        if (this.status != expected) {
            throw new IllegalStateException(
                    "Cannot " + action + " a purchase order in status: " + status + ". Expected: " + expected);
        }
    }
}
