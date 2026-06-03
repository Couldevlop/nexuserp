package com.nexuserp.payment.adapter.in.rest;

import com.nexuserp.payment.application.command.InitiatePaymentCommand;
import com.nexuserp.payment.domain.model.Payment;
import com.nexuserp.payment.domain.model.PaymentDirection;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public InitiatePaymentCommand toCommand(InitiatePaymentRequest req, String tenantId,
                                            String userId, String idempotencyKey) {
        return new InitiatePaymentCommand(
            tenantId,
            req.provider(),
            req.direction() != null ? req.direction() : PaymentDirection.COLLECTION,
            req.msisdn(),
            req.amount(),
            req.currency(),
            req.invoiceId(),
            req.description(),
            idempotencyKey,
            userId
        );
    }

    public PaymentDto toDto(Payment p) {
        return new PaymentDto(
            p.getId(),
            p.getReference(),
            p.getProvider().name(),
            p.getDirection().name(),
            p.getAccount().masked(),
            p.getAmount().amount(),
            p.getAmount().currency(),
            p.getStatus().name(),
            p.getExternalTxId(),
            p.getInvoiceId(),
            p.getFailureReason(),
            p.getCreatedAt(),
            p.getUpdatedAt()
        );
    }
}
