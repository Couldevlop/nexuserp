package com.nexuserp.payment.adapter.out.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexuserp.payment.domain.model.PaymentProvider;
import com.nexuserp.payment.domain.port.out.CallbackResult;
import com.nexuserp.payment.domain.port.out.PaymentInitiation;
import com.nexuserp.payment.domain.port.out.ProviderResponse;
import com.nexuserp.payment.infrastructure.config.PaymentProviderProperties;
import com.nexuserp.payment.infrastructure.config.PaymentProviderProperties.ProviderConfig;
import com.nexuserp.payment.infrastructure.config.PaymentProviderProperties.RealApiConfig;
import com.nexuserp.payment.infrastructure.config.ProviderConfigResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Test de la stratégie RÉELLE Wave avec un RestClient mocké (MockRestServiceServer) :
 *  - création de session checkout (happy path) -> redirectUrl,
 *  - vérification de signature webhook (pass / fail), constant-time.
 */
@DisplayName("WaveRealStrategy — RestClient mocké")
class WaveRealStrategyTest {

    private static final String SECRET = "wh_secret_test";
    private static final String BASE = "https://api.wave.com";

    private WaveRealStrategy strategy;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RealApiConfig real = new RealApiConfig(BASE, null, null, null, null,
            "wave-api-key", null, null, null, null,
            "https://shop/success", "https://shop/cancel", "XOF");
        ProviderConfig cfg = new ProviderConfig(SECRET, BASE, "wave-api-key", true, real);
        PaymentProviderProperties props =
            new PaymentProviderProperties("http://cb", Map.of("wave", cfg));

        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        // ConfigClient null : pas de store central en test -> props/env uniquement.
        strategy = new WaveRealStrategy(new ObjectMapper(),
            new ProviderConfigResolver(props, null), builder);
    }

    @Test
    @DisplayName("Should create checkout session and return wave_launch_url")
    void shouldCreateCheckoutSession() {
        server.expect(requestTo(BASE + "/v1/checkout/sessions"))
            .andExpect(method(POST))
            .andExpect(header("Authorization", "Bearer wave-api-key"))
            .andRespond(withSuccess("""
                { "id": "cos-123", "wave_launch_url": "https://pay.wave.com/c/cos-123" }
                """, APPLICATION_JSON));

        PaymentInitiation init = new PaymentInitiation(
            "ci-acme", "PAY-WAVE-1", PaymentProvider.WAVE, "+2250700000000",
            new BigDecimal("1500"), "XOF", "Test", "http://cb/wave");

        ProviderResponse resp = strategy.initiateCollection(init);

        server.verify();
        assertThat(resp.accepted()).isTrue();
        assertThat(resp.providerRef()).isEqualTo("cos-123");
        assertThat(resp.redirectUrl()).isEqualTo("https://pay.wave.com/c/cos-123");
    }

    @Test
    @DisplayName("Should return rejected when API call fails (fail closed)")
    void shouldRejectOnApiError() {
        server.expect(requestTo(BASE + "/v1/checkout/sessions"))
            .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                .withServerError());

        PaymentInitiation init = new PaymentInitiation(
            "ci-acme", "PAY-WAVE-2", PaymentProvider.WAVE, "+2250700000000",
            new BigDecimal("1500"), "XOF", "Test", "http://cb/wave");

        ProviderResponse resp = strategy.initiateCollection(init);
        assertThat(resp.accepted()).isFalse();
        assertThat(resp.rawMessage()).isEqualTo("WAVE_API_ERROR");
    }

    @Test
    @DisplayName("Should verify valid Wave-Signature (t=..,v1=..) and reject a tampered one")
    void shouldVerifyWaveSignature() {
        byte[] body = "{\"data\":{\"client_reference\":\"PAY-WAVE-1\",\"payment_status\":\"succeeded\"}}"
            .getBytes(StandardCharsets.UTF_8);
        String timestamp = "1700000000";
        byte[] signedPayload = concat((timestamp + ".").getBytes(StandardCharsets.UTF_8), body);
        String v1 = HmacSignatures.hmacSha256Hex(signedPayload, SECRET);

        String validHeader = "t=" + timestamp + ",v1=" + v1;
        assertThat(strategy.verifyCallback(body, validHeader)).isTrue();

        // Tampered signature -> false (fail closed).
        String badHeader = "t=" + timestamp + ",v1=" + v1.substring(0, v1.length() - 1) + "0";
        assertThat(strategy.verifyCallback(body, badHeader)).isFalse();
        // Missing header -> false.
        assertThat(strategy.verifyCallback(body, null)).isFalse();
    }

    @Test
    @DisplayName("Should parse Wave webhook into normalized CallbackResult")
    void shouldParseCallback() {
        byte[] body = ("""
            { "type":"checkout.session.completed",
              "data": { "id":"cos-123", "client_reference":"PAY-WAVE-1",
                        "payment_status":"succeeded", "amount":"1500", "currency":"XOF" } }
            """).getBytes(StandardCharsets.UTF_8);

        CallbackResult r = strategy.parseCallback(body);
        assertThat(r.reference()).isEqualTo("PAY-WAVE-1");
        assertThat(r.externalTxId()).isEqualTo("cos-123");
        assertThat(r.outcome()).isEqualTo(CallbackResult.Outcome.SUCCEEDED);
        assertThat(r.amount()).isEqualByComparingTo("1500");
        assertThat(r.currency()).isEqualTo("XOF");
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
