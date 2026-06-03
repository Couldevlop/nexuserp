package com.nexuserp.production.domain.model;

import com.nexuserp.core.domain.event.DomainEvent;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * WorkOrder (Ordre de Fabrication) aggregate — covers full MES lifecycle:
 * PLANNED → RELEASED → IN_PROGRESS → COMPLETED/CANCELLED
 */
@Getter
public class WorkOrder {

    public enum Status {
        PLANNED, RELEASED, IN_PROGRESS, PARTIALLY_COMPLETED,
        COMPLETED, ON_HOLD, CANCELLED
    }

    public enum Priority {
        LOW, NORMAL, HIGH, URGENT
    }

    private final String id;
    private final String tenantId;
    private final String orderNumber;
    private final String productId;
    private final String productName;
    private final String bomId;         // Bill of Materials
    private final String routingId;     // Gamme opératoire
    private final BigDecimal quantityPlanned;
    private BigDecimal quantityProduced;
    private BigDecimal quantityRejected;
    private Status status;
    private Priority priority;
    private final LocalDate plannedStartDate;
    private final LocalDate plannedEndDate;
    private LocalDateTime actualStartDate;
    private LocalDateTime actualEndDate;
    private String workcenter;
    private String operator;
    private String notes;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    @Builder
    private WorkOrder(String id, String tenantId, String orderNumber,
                      String productId, String productName, String bomId,
                      String routingId, BigDecimal quantityPlanned,
                      LocalDate plannedStartDate, LocalDate plannedEndDate,
                      Priority priority, String workcenter, String notes) {
        this.id = Objects.requireNonNull(id);
        this.tenantId = Objects.requireNonNull(tenantId);
        this.orderNumber = Objects.requireNonNull(orderNumber);
        this.productId = Objects.requireNonNull(productId);
        this.productName = Objects.requireNonNull(productName);
        this.bomId = bomId;
        this.routingId = routingId;
        this.quantityPlanned = Objects.requireNonNull(quantityPlanned);
        this.quantityProduced = BigDecimal.ZERO;
        this.quantityRejected = BigDecimal.ZERO;
        this.plannedStartDate = Objects.requireNonNull(plannedStartDate);
        this.plannedEndDate = Objects.requireNonNull(plannedEndDate);
        this.priority = priority != null ? priority : Priority.NORMAL;
        this.workcenter = workcenter;
        this.notes = notes;
        this.status = Status.PLANNED;
    }

    public void release(String userId) {
        assertStatus(Status.PLANNED, "release");
        this.status = Status.RELEASED;
    }

    public void start(String operatorId) {
        if (status != Status.RELEASED && status != Status.ON_HOLD) {
            throw new IllegalStateException("Cannot start work order in status: " + status);
        }
        this.operator = operatorId;
        this.actualStartDate = LocalDateTime.now();
        this.status = Status.IN_PROGRESS;
    }

    public void recordProduction(BigDecimal quantity, BigDecimal rejected, String operatorId) {
        if (status != Status.IN_PROGRESS && status != Status.PARTIALLY_COMPLETED) {
            throw new IllegalStateException("Cannot record production for status: " + status);
        }
        if (quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Produced quantity cannot be negative");
        }
        if (rejected != null && rejected.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Rejected quantity cannot be negative");
        }
        this.quantityProduced = this.quantityProduced.add(quantity);
        if (rejected != null) {
            this.quantityRejected = this.quantityRejected.add(rejected);
        }
        this.operator = operatorId;

        if (this.quantityProduced.compareTo(this.quantityPlanned) >= 0) {
            this.actualEndDate = LocalDateTime.now();
            this.status = Status.COMPLETED;
        } else {
            this.status = Status.PARTIALLY_COMPLETED;
        }
    }

    public void putOnHold(String reason, String userId) {
        if (status != Status.IN_PROGRESS && status != Status.PARTIALLY_COMPLETED) {
            throw new IllegalStateException("Cannot put on hold: status is " + status);
        }
        this.status = Status.ON_HOLD;
        this.notes = (notes != null ? notes + "\n" : "") + "ON HOLD: " + reason;
    }

    public void cancel(String reason, String userId) {
        if (status == Status.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed work order");
        }
        this.status = Status.CANCELLED;
    }

    public BigDecimal getYieldRate() {
        if (quantityPlanned.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return quantityProduced.subtract(quantityRejected)
                .divide(quantityPlanned, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    public BigDecimal getRemainingQuantity() {
        return quantityPlanned.subtract(quantityProduced).max(BigDecimal.ZERO);
    }

    public boolean isLate() {
        return status != Status.COMPLETED && status != Status.CANCELLED
                && LocalDate.now().isAfter(plannedEndDate);
    }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    private void assertStatus(Status expected, String action) {
        if (this.status != expected) {
            throw new IllegalStateException(
                    "Cannot " + action + " a work order in status: " + status + ". Expected: " + expected);
        }
    }
}
