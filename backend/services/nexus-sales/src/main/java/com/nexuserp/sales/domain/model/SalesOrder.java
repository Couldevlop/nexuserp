package com.nexuserp.sales.domain.model;

import com.nexuserp.core.domain.exception.DomainException;
import com.nexuserp.core.domain.value.Money;
import com.nexuserp.core.domain.value.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Agrégat Commande Vente — Cycle : Devis → Commande → Livraison → Facture → Paiement.
 */
public class SalesOrder {

    public enum OrderStatus {
        DRAFT, CONFIRMED, PICKING, SHIPPED, DELIVERED, INVOICED, CANCELLED
    }

    private final UUID id;
    private final TenantId tenantId;
    private final String orderNumber;
    private OrderStatus status;

    private UUID customerId;
    private String customerName;
    private String customerRef;      // Référence commande client

    private LocalDate orderDate;
    private LocalDate requestedDeliveryDate;
    private LocalDate confirmedDeliveryDate;
    private LocalDate actualDeliveryDate;

    private final String currency;
    private Money subtotal;
    private Money taxAmount;
    private Money total;
    private Money shippingCost;

    private String shippingAddress;
    private String notes;
    private String internalNotes;
    private String createdBy;
    private String assignedTo;       // Commercial responsable

    private final List<SalesOrderLine> lines;
    private final List<Object> domainEvents;

    private SalesOrder(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID();
        this.tenantId = builder.tenantId;
        this.orderNumber = builder.orderNumber;
        this.status = OrderStatus.DRAFT;
        this.customerId = builder.customerId;
        this.customerName = builder.customerName;
        this.customerRef = builder.customerRef;
        this.orderDate = builder.orderDate != null ? builder.orderDate : LocalDate.now();
        this.requestedDeliveryDate = builder.requestedDeliveryDate;
        this.currency = builder.currency != null ? builder.currency : "EUR";
        this.notes = builder.notes;
        this.internalNotes = builder.internalNotes;
        this.createdBy = builder.createdBy;
        this.assignedTo = builder.assignedTo;
        this.shippingAddress = builder.shippingAddress;
        this.shippingCost = builder.shippingCost != null ? builder.shippingCost : Money.zero(this.currency);
        this.lines = new ArrayList<>();
        this.domainEvents = new ArrayList<>();
        builder.lines.forEach(this::addLine);
    }

    public void addLine(SalesOrderLine line) {
        if (status != OrderStatus.DRAFT) {
            throw DomainException.invalidState("SalesOrder", status.name(), "DRAFT");
        }
        lines.add(line);
        recalculate();
    }

    public void confirm() {
        if (status != OrderStatus.DRAFT) {
            throw DomainException.invalidState("SalesOrder", status.name(), "DRAFT");
        }
        if (lines.isEmpty()) {
            throw DomainException.of("ORDER_NO_LINES", "Sales order must have at least one line");
        }
        this.status = OrderStatus.CONFIRMED;
    }

    public void startPicking() {
        if (status != OrderStatus.CONFIRMED) {
            throw DomainException.invalidState("SalesOrder", status.name(), "CONFIRMED");
        }
        this.status = OrderStatus.PICKING;
    }

    public void ship(LocalDate shipDate) {
        if (status != OrderStatus.PICKING) {
            throw DomainException.invalidState("SalesOrder", status.name(), "PICKING");
        }
        this.status = OrderStatus.SHIPPED;
    }

    public void deliver(LocalDate deliveryDate) {
        if (status != OrderStatus.SHIPPED) {
            throw DomainException.invalidState("SalesOrder", status.name(), "SHIPPED");
        }
        this.actualDeliveryDate = deliveryDate;
        this.status = OrderStatus.DELIVERED;
    }

    public void markInvoiced() {
        if (status != OrderStatus.DELIVERED) {
            throw DomainException.invalidState("SalesOrder", status.name(), "DELIVERED");
        }
        this.status = OrderStatus.INVOICED;
    }

    public void cancel(String reason) {
        if (status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED || status == OrderStatus.INVOICED) {
            throw DomainException.of("CANNOT_CANCEL", "Cannot cancel order in status: " + status);
        }
        this.status = OrderStatus.CANCELLED;
    }

    private void recalculate() {
        BigDecimal subtotalSum = lines.stream()
            .map(SalesOrderLine::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taxSum = lines.stream()
            .map(SalesOrderLine::getTaxAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.subtotal = Money.of(subtotalSum, currency);
        this.taxAmount = Money.of(taxSum, currency);
        BigDecimal shipping = shippingCost != null ? shippingCost.amount() : BigDecimal.ZERO;
        this.total = Money.of(subtotalSum.add(taxSum).add(shipping), currency);
    }

    // Getters
    public UUID getId() { return id; }
    public TenantId getTenantId() { return tenantId; }
    public String getOrderNumber() { return orderNumber; }
    public OrderStatus getStatus() { return status; }
    public UUID getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public String getCustomerRef() { return customerRef; }
    public LocalDate getOrderDate() { return orderDate; }
    public LocalDate getRequestedDeliveryDate() { return requestedDeliveryDate; }
    public LocalDate getConfirmedDeliveryDate() { return confirmedDeliveryDate; }
    public LocalDate getActualDeliveryDate() { return actualDeliveryDate; }
    public String getCurrency() { return currency; }
    public Money getSubtotal() { return subtotal; }
    public Money getTaxAmount() { return taxAmount; }
    public Money getTotal() { return total; }
    public Money getShippingCost() { return shippingCost; }
    public String getShippingAddress() { return shippingAddress; }
    public String getNotes() { return notes; }
    public String getInternalNotes() { return internalNotes; }
    public String getCreatedBy() { return createdBy; }
    public String getAssignedTo() { return assignedTo; }
    public List<SalesOrderLine> getLines() { return Collections.unmodifiableList(lines); }
    public List<Object> getDomainEvents() { return Collections.unmodifiableList(domainEvents); }
    public void clearDomainEvents() { domainEvents.clear(); }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private TenantId tenantId;
        private String orderNumber;
        private UUID customerId;
        private String customerName;
        private String customerRef;
        private LocalDate orderDate;
        private LocalDate requestedDeliveryDate;
        private String currency;
        private String notes;
        private String internalNotes;
        private String createdBy;
        private String assignedTo;
        private String shippingAddress;
        private Money shippingCost;
        private final List<SalesOrderLine> lines = new ArrayList<>();

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder tenantId(String t) { this.tenantId = TenantId.of(t); return this; }
        public Builder orderNumber(String n) { this.orderNumber = n; return this; }
        public Builder customerId(UUID id) { this.customerId = id; return this; }
        public Builder customerName(String n) { this.customerName = n; return this; }
        public Builder customerRef(String r) { this.customerRef = r; return this; }
        public Builder orderDate(LocalDate d) { this.orderDate = d; return this; }
        public Builder requestedDeliveryDate(LocalDate d) { this.requestedDeliveryDate = d; return this; }
        public Builder currency(String c) { this.currency = c; return this; }
        public Builder notes(String n) { this.notes = n; return this; }
        public Builder internalNotes(String n) { this.internalNotes = n; return this; }
        public Builder createdBy(String u) { this.createdBy = u; return this; }
        public Builder assignedTo(String u) { this.assignedTo = u; return this; }
        public Builder shippingAddress(String a) { this.shippingAddress = a; return this; }
        public Builder shippingCost(Money m) { this.shippingCost = m; return this; }
        public Builder addLine(SalesOrderLine l) { this.lines.add(l); return this; }

        public SalesOrder build() {
            if (tenantId == null) throw new IllegalStateException("tenantId required");
            if (orderNumber == null) throw new IllegalStateException("orderNumber required");
            if (createdBy == null) throw new IllegalStateException("createdBy required");
            return new SalesOrder(this);
        }
    }
}
