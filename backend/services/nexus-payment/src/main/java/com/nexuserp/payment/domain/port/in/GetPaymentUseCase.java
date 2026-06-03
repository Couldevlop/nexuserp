package com.nexuserp.payment.domain.port.in;

import com.nexuserp.payment.application.query.PaymentPageQuery;
import com.nexuserp.payment.domain.model.Payment;
import org.springframework.data.domain.Page;

import java.util.UUID;

/**
 * Port IN — lecture des paiements (scoppé tenant).
 */
public interface GetPaymentUseCase {
    Payment getById(UUID id, String tenantId);
    Page<Payment> getPayments(PaymentPageQuery query);
}
