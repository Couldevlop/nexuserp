package com.nexuserp.payment.domain.service;

import com.nexuserp.core.domain.exception.DomainException;
import com.nexuserp.core.domain.value.Money;
import com.nexuserp.payment.application.command.InitiatePaymentCommand;
import com.nexuserp.payment.application.query.PaymentPageQuery;
import com.nexuserp.payment.domain.model.MobileMoneyAccount;
import com.nexuserp.payment.domain.model.Payment;
import com.nexuserp.payment.domain.model.PaymentProvider;
import com.nexuserp.payment.domain.port.in.GetPaymentUseCase;
import com.nexuserp.payment.domain.port.in.HandlePaymentCallbackUseCase;
import com.nexuserp.payment.domain.port.in.InitiatePaymentUseCase;
import com.nexuserp.payment.domain.port.out.CallbackResult;
import com.nexuserp.payment.domain.port.out.PaymentEventPublisher;
import com.nexuserp.payment.domain.port.out.PaymentInitiation;
import com.nexuserp.payment.domain.port.out.PaymentProviderGateway;
import com.nexuserp.payment.domain.port.out.PaymentRepository;
import com.nexuserp.payment.domain.port.out.ProviderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service domaine Paiement — implémente les Use Cases Mobile Money.
 * La logique métier réside ici, pas dans les contrôleurs ni l'entité JPA.
 *
 * OWASP :
 *  - A04 : idempotencyKey + référence uniques par tenant -> pas de double-débit.
 *  - A08 : aucune mutation d'état avant vérification de signature du webhook.
 *  - A09 : journalisation du cycle de vie sans MSISDN complet ni secret.
 */
@Service
@Transactional
public class PaymentService implements InitiatePaymentUseCase, HandlePaymentCallbackUseCase, GetPaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final DateTimeFormatter REF_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String DEFAULT_CURRENCY = "XOF";

    private final PaymentRepository paymentRepository;
    private final PaymentProviderGateway providerGateway;
    private final PaymentEventPublisher eventPublisher;

    private final String callbackBaseUrl;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentProviderGateway providerGateway,
                          PaymentEventPublisher eventPublisher,
                          @Value("${nexus.payment.callback-base-url:http://localhost:8093/api/v1/payments/webhooks}") String callbackBaseUrl) {
        this.paymentRepository = paymentRepository;
        this.providerGateway = providerGateway;
        this.eventPublisher = eventPublisher;
        this.callbackBaseUrl = callbackBaseUrl;
    }

    // ─── Initiation ─────────────────────────────────────────────────────────────

    @Override
    public Payment initiate(InitiatePaymentCommand cmd) {
        String currency = cmd.currency() != null ? cmd.currency() : DEFAULT_CURRENCY;
        String idempotencyKey = (cmd.idempotencyKey() != null && !cmd.idempotencyKey().isBlank())
            ? cmd.idempotencyKey()
            : UUID.randomUUID().toString();

        // A04 — Idempotence : si une demande identique existe, on la retourne sans re-débit.
        Optional<Payment> existing = paymentRepository
            .findByTenantIdAndIdempotencyKey(cmd.tenantId(), idempotencyKey);
        if (existing.isPresent()) {
            log.info("Idempotent initiate: returning existing payment id={}, ref={}, tenant={}",
                existing.get().getId(), existing.get().getReference(), cmd.tenantId());
            return existing.get();
        }

        MobileMoneyAccount account = MobileMoneyAccount.of(cmd.msisdn()); // valide & normalise (A03)
        Money amount = Money.of(cmd.amount(), currency);
        String reference = generateReference(cmd.provider());

        Payment payment = Payment.builder()
            .tenantId(cmd.tenantId())
            .reference(reference)
            .provider(cmd.provider())
            .direction(cmd.direction())
            .account(account)
            .amount(amount)
            .invoiceId(cmd.invoiceId())
            .idempotencyKey(idempotencyKey)
            .createdBy(cmd.createdBy())
            .build();

        // Persistance en PENDING avant l'appel provider (durabilité + contrainte d'unicité).
        Payment saved = paymentRepository.save(payment);
        log.info("Payment created PENDING id={}, ref={}, provider={}, msisdn={}, amount={}, tenant={}",
            saved.getId(), reference, cmd.provider(), account.masked(), amount, cmd.tenantId());

        // Appel provider Mobile Money.
        PaymentInitiation initiation = new PaymentInitiation(
            cmd.tenantId(), reference, cmd.provider(), account.value(),
            cmd.amount(), currency, cmd.description(),
            callbackBaseUrl + "/" + cmd.provider().name().toLowerCase());

        ProviderResponse response;
        try {
            response = providerGateway.initiateCollection(initiation);
        } catch (RuntimeException ex) {
            saved.markFailed("PROVIDER_ERROR: " + ex.getClass().getSimpleName());
            Payment failed = paymentRepository.save(saved);
            eventPublisher.publishAll(failed.getDomainEvents());
            failed.clearDomainEvents();
            log.warn("Provider initiation threw for ref={}, marked FAILED", reference);
            return failed;
        }

        if (response.accepted()) {
            saved.initiate(response.providerRef());
        } else {
            saved.markFailed("PROVIDER_REJECTED: " + response.rawMessage());
        }

        Payment result = paymentRepository.save(saved);
        eventPublisher.publishAll(result.getDomainEvents());
        result.clearDomainEvents();

        log.info("Payment ref={} status={} after provider call", reference, result.getStatus());
        return result;
    }

    // ─── Callback / Webhook ───────────────────────────────────────────────────

    @Override
    public void handleCallback(PaymentProvider provider, byte[] rawBody, String signatureHeader) {
        // A02/A08 — Vérifier l'authenticité AVANT toute lecture/mutation.
        boolean authentic = providerGateway.verifyCallback(rawBody, signatureHeader, provider);
        if (!authentic) {
            log.warn("Rejected {} webhook: invalid signature", provider);
            throw DomainException.of("SIGNATURE_INVALID", "Webhook signature verification failed");
        }

        CallbackResult result = providerGateway.parseCallback(rawBody, provider);
        if (result == null || result.reference() == null) {
            log.warn("Rejected {} webhook: unparsable or missing reference", provider);
            throw DomainException.of("CALLBACK_UNPARSABLE", "Callback could not be parsed");
        }

        Payment payment = paymentRepository.findByReference(result.reference())
            .orElseThrow(() -> DomainException.notFound("Payment", result.reference()));

        // A04 — Idempotence : si déjà dans un état terminal correspondant, no-op.
        switch (result.outcome()) {
            case SUCCEEDED -> {
                payment.markSucceeded(result.externalTxId()); // no-op si déjà SUCCEEDED
            }
            case FAILED -> {
                payment.markFailed(result.failureReason() != null
                    ? result.failureReason() : "PROVIDER_REPORTED_FAILURE");
            }
            case UNKNOWN -> {
                log.info("Webhook for ref={} carries UNKNOWN outcome (status={}) — ignored",
                    result.reference(), result.rawStatus());
                return;
            }
        }

        Payment saved = paymentRepository.save(payment);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();

        log.info("Webhook processed: provider={}, ref={}, status={}",
            provider, result.reference(), saved.getStatus());
    }

    // ─── Queries ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Payment getById(UUID id, String tenantId) {
        return paymentRepository.findById(id, tenantId)
            .orElseThrow(() -> DomainException.notFound("Payment", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Payment> getPayments(PaymentPageQuery query) {
        return paymentRepository.findAll(query.tenantId(), query.status(), query.toPageable());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Référence lisible et unique par tenant : PAY-<PROVIDER>-<yyyyMMdd>-<random>.
     */
    private String generateReference(PaymentProvider provider) {
        String suffix = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000))
            + Long.toString(System.nanoTime() % 100_000, 36).toUpperCase();
        return "PAY-" + provider.name() + "-" + LocalDate.now().format(REF_DATE) + "-" + suffix;
    }
}
