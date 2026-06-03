package com.nexuserp.payment.domain.port.in;

import com.nexuserp.payment.application.command.InitiatePaymentCommand;
import com.nexuserp.payment.domain.model.Payment;

/**
 * Port IN — initiation d'un paiement Mobile Money.
 */
public interface InitiatePaymentUseCase {
    Payment initiate(InitiatePaymentCommand command);
}
