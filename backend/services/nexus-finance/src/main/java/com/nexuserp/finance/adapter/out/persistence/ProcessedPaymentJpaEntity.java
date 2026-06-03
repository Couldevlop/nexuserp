package com.nexuserp.finance.adapter.out.persistence;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Marqueur d'idempotence : un paiement externe (paymentId) appliqué une seule fois par tenant.
 */
@Entity
@Table(name = "processed_payments", schema = "nexus_finance",
    uniqueConstraints = @UniqueConstraint(name = "uq_processed_payment", columnNames = {"tenant_id", "payment_id"}))
public class ProcessedPaymentJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false, length = 200)
    private String id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "payment_id", nullable = false, length = 100)
    private String paymentId;

    @Column(name = "invoice_id", length = 100)
    private String invoiceId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedPaymentJpaEntity() {}

    public ProcessedPaymentJpaEntity(String tenantId, String paymentId, String invoiceId) {
        this.id = tenantId + ":" + paymentId;
        this.tenantId = tenantId;
        this.paymentId = paymentId;
        this.invoiceId = invoiceId;
        this.processedAt = Instant.now();
    }

    public String getId() { return id; }
    public String getTenantId() { return tenantId; }
    public String getPaymentId() { return paymentId; }
    public String getInvoiceId() { return invoiceId; }
    public Instant getProcessedAt() { return processedAt; }
}
