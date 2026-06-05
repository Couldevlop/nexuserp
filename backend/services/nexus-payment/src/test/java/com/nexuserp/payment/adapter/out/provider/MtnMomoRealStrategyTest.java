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
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Test de la stratégie RÉELLE MTN MoMo (token Basic -> requestToPay) avec RestClient mocké.
 */
@DisplayName("MtnMomoRealStrategy — token + requestToPay mockés")
class MtnMomoRealStrategyTest {

    private static final String BASE = "https://sandbox.momodeveloper.mtn.com";
    private static final String SECRET = "mtn_wh_secret";

    private MtnMomoRealStrategy strategy;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RealApiConfig real = new RealApiConfig(BASE, null, null, null, null, null,
            "api-user-id", "api-user-key", "sub-key", "sandbox", null, null, "EUR");
        ProviderConfig cfg = new ProviderConfig(SECRET, BASE, "k", true, real);
        PaymentProviderProperties props =
            new PaymentProviderProperties("http://cb", Map.of("mtn_momo", cfg));

        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        // ConfigClient null : pas de store central en test -> props/env uniquement.
        strategy = new MtnMomoRealStrategy(new ObjectMapper(),
            new ProviderConfigResolver(props, null), builder);
    }

    @Test
    @DisplayName("Should fetch token then requestToPay (202) and return X-Reference-Id")
    void shouldRequestToPay() {
        server.expect(requestTo(BASE + "/collection/token/"))
            .andExpect(method(POST))
            .andExpect(header("Ocp-Apim-Subscription-Key", "sub-key"))
            .andRespond(withSuccess("{ \"access_token\": \"tok-xyz\", \"expires_in\": 3600 }",
                APPLICATION_JSON));

        server.expect(requestTo(BASE + "/collection/v1_0/requesttopay"))
            .andExpect(method(POST))
            .andExpect(header("Authorization", "Bearer tok-xyz"))
            .andExpect(header("X-Target-Environment", "sandbox"))
            .andExpect(header("Ocp-Apim-Subscription-Key", "sub-key"))
            .andRespond(withStatus(org.springframework.http.HttpStatus.ACCEPTED));

        PaymentInitiation init = new PaymentInitiation(
            "ci-acme", "PAY-MTN-1", PaymentProvider.MTN_MOMO, "+2250700000000",
            new BigDecimal("2000"), "EUR", "Test MTN", "http://cb/mtn");

        ProviderResponse resp = strategy.initiateCollection(init);

        server.verify();
        assertThat(resp.accepted()).isTrue();
        assertThat(resp.providerRef()).isNotBlank(); // X-Reference-Id (UUID)
    }

    @Test
    @DisplayName("Should verify HMAC webhook signature and parse MTN callback")
    void shouldVerifyAndParse() {
        byte[] body = ("""
            { "externalId":"PAY-MTN-1", "financialTransactionId":"ft-9",
              "status":"SUCCESSFUL", "amount":"2000", "currency":"EUR" }
            """).getBytes(StandardCharsets.UTF_8);
        String sig = HmacSignatures.hmacSha256Hex(body, SECRET);

        assertThat(strategy.verifyCallback(body, sig)).isTrue();
        assertThat(strategy.verifyCallback(body, "deadbeef")).isFalse();

        CallbackResult r = strategy.parseCallback(body);
        assertThat(r.reference()).isEqualTo("PAY-MTN-1");
        assertThat(r.externalTxId()).isEqualTo("ft-9");
        assertThat(r.outcome()).isEqualTo(CallbackResult.Outcome.SUCCEEDED);
    }
}
