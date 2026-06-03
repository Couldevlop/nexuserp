package com.nexuserp.payment.application;

import com.nexuserp.core.domain.exception.DomainException;
import com.nexuserp.core.domain.value.Money;
import com.nexuserp.payment.application.command.InitiatePaymentCommand;
import com.nexuserp.payment.domain.model.MobileMoneyAccount;
import com.nexuserp.payment.domain.model.Payment;
import com.nexuserp.payment.domain.model.PaymentDirection;
import com.nexuserp.payment.domain.model.PaymentProvider;
import com.nexuserp.payment.domain.model.PaymentStatus;
import com.nexuserp.payment.domain.port.out.CallbackResult;
import com.nexuserp.payment.domain.port.out.PaymentEventPublisher;
import com.nexuserp.payment.domain.port.out.PaymentProviderGateway;
import com.nexuserp.payment.domain.port.out.PaymentRepository;
import com.nexuserp.payment.domain.port.out.ProviderResponse;
import com.nexuserp.payment.domain.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService — Use Case Tests")
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentProviderGateway providerGateway;
    @Mock private PaymentEventPublisher eventPublisher;

    private PaymentService service;

    private static final String TENANT = "ci-acme";
    private static final String CALLBACK_BASE = "http://localhost:8093/api/v1/payments/webhooks";

    @BeforeEach
    void setUp() {
        service = new PaymentService(paymentRepository, providerGateway, eventPublisher, CALLBACK_BASE);
    }

    // ─── Initiation ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should initiate payment, persist INITIATED and publish event")
    void shouldInitiate_whenProviderAccepts() {
        when(paymentRepository.findByTenantIdAndIdempotencyKey(eq(TENANT), anyString()))
            .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(providerGateway.initiateCollection(any()))
            .thenReturn(ProviderResponse.accepted("orange-ref-123", null, "USSD"));

        InitiatePaymentCommand cmd = new InitiatePaymentCommand(
            TENANT, PaymentProvider.ORANGE_MONEY, PaymentDirection.COLLECTION,
            "+2250700000000", new BigDecimal("1000"), "XOF",
            null, "Test collection", "idem-1", "user-1");

        Payment result = service.initiate(cmd);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.INITIATED);
        assertThat(result.getExternalTxId()).isEqualTo("orange-ref-123");
        assertThat(result.getReference()).startsWith("PAY-ORANGE_MONEY-");
        // PENDING save then INITIATED save.
        verify(paymentRepository, org.mockito.Mockito.times(2)).save(any(Payment.class));
        verify(eventPublisher).publishAll(any());
    }

    @Test
    @DisplayName("Should be idempotent: return existing payment without provider call")
    void shouldReturnExisting_whenIdempotencyKeyMatches() {
        Payment existing = newPayment("idem-dup");
        when(paymentRepository.findByTenantIdAndIdempotencyKey(TENANT, "idem-dup"))
            .thenReturn(Optional.of(existing));

        InitiatePaymentCommand cmd = new InitiatePaymentCommand(
            TENANT, PaymentProvider.WAVE, PaymentDirection.COLLECTION,
            "+2250700000000", new BigDecimal("500"), "XOF",
            null, "dup", "idem-dup", "user-1");

        Payment result = service.initiate(cmd);

        assertThat(result).isSameAs(existing);
        verify(providerGateway, never()).initiateCollection(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should mark FAILED when provider rejects")
    void shouldMarkFailed_whenProviderRejects() {
        when(paymentRepository.findByTenantIdAndIdempotencyKey(eq(TENANT), anyString()))
            .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(providerGateway.initiateCollection(any()))
            .thenReturn(ProviderResponse.rejected("INSUFFICIENT_FUNDS"));

        InitiatePaymentCommand cmd = new InitiatePaymentCommand(
            TENANT, PaymentProvider.MTN_MOMO, PaymentDirection.COLLECTION,
            "+2250700000000", new BigDecimal("1000"), "XOF",
            null, "x", "idem-2", "user-1");

        Payment result = service.initiate(cmd);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.getFailureReason()).contains("INSUFFICIENT_FUNDS");
    }

    @Test
    @DisplayName("Should reject initiation with invalid MSISDN (A03)")
    void shouldThrow_whenInvalidMsisdn() {
        when(paymentRepository.findByTenantIdAndIdempotencyKey(eq(TENANT), anyString()))
            .thenReturn(Optional.empty());

        InitiatePaymentCommand cmd = new InitiatePaymentCommand(
            TENANT, PaymentProvider.ORANGE_MONEY, PaymentDirection.COLLECTION,
            "abc", new BigDecimal("1000"), "XOF", null, "x", "idem-3", "user-1");

        assertThatThrownBy(() -> service.initiate(cmd))
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("MSISDN");
        verify(paymentRepository, never()).save(any());
    }

    // ─── Callbacks ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should reject callback with invalid signature without state change (A08)")
    void shouldReject_whenSignatureInvalid() {
        when(providerGateway.verifyCallback(any(), anyString(), eq(PaymentProvider.ORANGE_MONEY)))
            .thenReturn(false);

        assertThatThrownBy(() -> service.handleCallback(
            PaymentProvider.ORANGE_MONEY, "{}".getBytes(), "bad-sig"))
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("signature");

        verify(paymentRepository, never()).findByReference(anyString());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should mark SUCCEEDED and publish on valid success callback")
    void shouldSucceed_onValidCallback() {
        Payment pending = newPayment("idem-s");
        when(providerGateway.verifyCallback(any(), anyString(), any())).thenReturn(true);
        when(providerGateway.parseCallback(any(), any())).thenReturn(new CallbackResult(
            pending.getReference(), "ext-999", CallbackResult.Outcome.SUCCEEDED,
            new BigDecimal("1000"), "XOF", "SUCCESS", null));
        when(paymentRepository.findByReference(pending.getReference())).thenReturn(Optional.of(pending));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        service.handleCallback(PaymentProvider.WAVE, "{}".getBytes(), "good-sig");

        assertThat(pending.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(pending.getExternalTxId()).isEqualTo("ext-999");
        // A PaymentSucceededEvent was accumulated before publish (verified via state + publish call).
        verify(eventPublisher).publishAll(any());
    }

    @Test
    @DisplayName("Should mark FAILED on valid failure callback")
    void shouldFail_onValidCallback() {
        Payment pending = newPayment("idem-f");
        when(providerGateway.verifyCallback(any(), anyString(), any())).thenReturn(true);
        when(providerGateway.parseCallback(any(), any())).thenReturn(new CallbackResult(
            pending.getReference(), null, CallbackResult.Outcome.FAILED,
            new BigDecimal("1000"), "XOF", "FAILED", "TIMEOUT"));
        when(paymentRepository.findByReference(pending.getReference())).thenReturn(Optional.of(pending));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        service.handleCallback(PaymentProvider.MTN_MOMO, "{}".getBytes(), "good-sig");

        assertThat(pending.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(pending.getFailureReason()).isEqualTo("TIMEOUT");
    }

    @Test
    @DisplayName("Should be a no-op on duplicate success callback (A04 idempotency)")
    void shouldNoOp_onDuplicateSuccessCallback() {
        Payment already = newPayment("idem-d");
        already.initiate("ref-x");
        already.markSucceeded("ext-1");
        already.clearDomainEvents();

        when(providerGateway.verifyCallback(any(), anyString(), any())).thenReturn(true);
        when(providerGateway.parseCallback(any(), any())).thenReturn(new CallbackResult(
            already.getReference(), "ext-1", CallbackResult.Outcome.SUCCEEDED,
            new BigDecimal("1000"), "XOF", "SUCCESS", null));
        when(paymentRepository.findByReference(already.getReference())).thenReturn(Optional.of(already));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        service.handleCallback(PaymentProvider.ORANGE_MONEY, "{}".getBytes(), "good-sig");

        // Statut inchangé et aucun nouvel événement (la liste publiée est vide).
        assertThat(already.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        verify(eventPublisher).publishAll(argThatIsEmpty());
    }

    // ─── State machine guard ────────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw on illegal state transition (initiate from SUCCEEDED)")
    void shouldThrow_onIllegalTransition() {
        Payment p = newPayment("idem-t");
        p.initiate("ref");
        p.markSucceeded("ext");

        assertThatThrownBy(() -> p.initiate("ref2"))
            .isInstanceOf(DomainException.class);
        // SUCCEEDED -> REFUNDED is a legal transition (no throw).
        org.assertj.core.api.Assertions.assertThatCode(p::markRefunded).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should throw when cancelling a SUCCEEDED payment")
    void shouldThrow_whenCancellingSucceeded() {
        Payment p = newPayment("idem-c");
        p.initiate("ref");
        p.markSucceeded("ext");

        assertThatThrownBy(() -> p.cancel("late"))
            .isInstanceOf(DomainException.class);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Payment newPayment(String idem) {
        return Payment.builder()
            .tenantId(TENANT)
            .reference("PAY-ORANGE_MONEY-20260101-TEST" + idem)
            .provider(PaymentProvider.ORANGE_MONEY)
            .account(MobileMoneyAccount.of("+2250700000000"))
            .amount(Money.of(new BigDecimal("1000"), "XOF"))
            .idempotencyKey(idem)
            .createdBy("user-1")
            .build();
    }

    private static java.util.List<Object> argThatIsEmpty() {
        return org.mockito.ArgumentMatchers.argThat(java.util.List::isEmpty);
    }
}
