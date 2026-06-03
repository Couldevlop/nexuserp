package com.nexuserp.finance.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexuserp.finance.adapter.in.rest.dto.CreateInvoiceRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Invoice Integration Tests — full stack with TestContainers")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InvoiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("nexus_finance_test")
                    .withUsername("nexus")
                    .withPassword("nexus_test");

    @Container
    static final KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @Container
    static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("nexuserp.tenant.default", () -> "integration-test");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String createdInvoiceId;

    @Test
    @Order(1)
    @DisplayName("Should create invoice via REST API")
    void shouldCreateInvoice_viaRestApi() throws Exception {
        CreateInvoiceRequest request = buildCreateRequest();

        MvcResult result = mockMvc.perform(
                        post("/api/v1/finance/invoices")
                                .with(jwt()
                                        .jwt(jwt -> jwt
                                                .claim("tenantId", "fr-integration")
                                                .claim("sub", "user-001")
                                                .claim("realm_access",
                                                        java.util.Map.of("roles", List.of("FINANCE_USER", "FINANCE_MANAGER")))
                                        ))
                                .header("X-Tenant-ID", "fr-integration")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.invoiceNumber").value(
                        org.hamcrest.Matchers.matchesPattern("FA-\\d{4}-.*")))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.customerName").value("Integration Test SA"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        createdInvoiceId = objectMapper.readTree(responseBody)
                .path("data").path("id").asText();

        assertThat(createdInvoiceId).isNotEmpty();
    }

    @Test
    @Order(2)
    @DisplayName("Should retrieve created invoice by ID")
    void shouldRetrieveInvoice_byId() throws Exception {
        assumeCreatedInvoiceExists();

        mockMvc.perform(
                        get("/api/v1/finance/invoices/{id}", createdInvoiceId)
                                .with(jwt()
                                        .jwt(jwt -> jwt
                                                .claim("tenantId", "fr-integration")
                                                .claim("realm_access",
                                                        java.util.Map.of("roles", List.of("FINANCE_USER")))
                                        ))
                                .header("X-Tenant-ID", "fr-integration")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(createdInvoiceId))
                .andExpect(jsonPath("$.data.totalAmount").isNumber());
    }

    @Test
    @Order(3)
    @DisplayName("Should approve invoice when role is FINANCE_MANAGER")
    void shouldApproveInvoice_whenFinanceManager() throws Exception {
        assumeCreatedInvoiceExists();

        // First submit the invoice (simulated via direct state — test helper)
        // In real scenario, submit is a separate endpoint or auto-submit on create
        // Here we approve directly to test the approval flow

        mockMvc.perform(
                        post("/api/v1/finance/invoices/{id}/approve", createdInvoiceId)
                                .with(jwt()
                                        .jwt(jwt -> jwt
                                                .claim("tenantId", "fr-integration")
                                                .claim("sub", "manager-001")
                                                .claim("realm_access",
                                                        java.util.Map.of("roles", List.of("FINANCE_MANAGER")))
                                        ))
                                .header("X-Tenant-ID", "fr-integration")
                )
                .andExpect(status().isOk());
    }

    @Test
    @Order(4)
    @DisplayName("Should return 403 when FINANCE_USER tries to approve")
    void shouldReturn403_whenFinanceUserTriesToApprove() throws Exception {
        assumeCreatedInvoiceExists();

        mockMvc.perform(
                        post("/api/v1/finance/invoices/{id}/approve", createdInvoiceId)
                                .with(jwt()
                                        .jwt(jwt -> jwt
                                                .claim("tenantId", "fr-integration")
                                                .claim("realm_access",
                                                        java.util.Map.of("roles", List.of("FINANCE_USER")))
                                        ))
                                .header("X-Tenant-ID", "fr-integration")
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(5)
    @DisplayName("Should list invoices with pagination")
    void shouldListInvoices_withPagination() throws Exception {
        mockMvc.perform(
                        get("/api/v1/finance/invoices")
                                .param("page", "0")
                                .param("size", "10")
                                .param("sort", "invoiceDate,desc")
                                .with(jwt()
                                        .jwt(jwt -> jwt
                                                .claim("tenantId", "fr-integration")
                                                .claim("realm_access",
                                                        java.util.Map.of("roles", List.of("FINANCE_USER")))
                                        ))
                                .header("X-Tenant-ID", "fr-integration")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.meta.page").value(0))
                .andExpect(jsonPath("$.meta.size").value(10));
    }

    @Test
    @Order(6)
    @DisplayName("Multi-tenant isolation — tenant-B cannot see tenant-A invoices")
    void shouldIsolateInvoices_betweenTenants() throws Exception {
        assumeCreatedInvoiceExists();

        // Tenant-B should NOT see invoice created by fr-integration
        mockMvc.perform(
                        get("/api/v1/finance/invoices/{id}", createdInvoiceId)
                                .with(jwt()
                                        .jwt(jwt -> jwt
                                                .claim("tenantId", "other-tenant")
                                                .claim("realm_access",
                                                        java.util.Map.of("roles", List.of("FINANCE_USER")))
                                        ))
                                .header("X-Tenant-ID", "other-tenant")
                )
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(7)
    @DisplayName("Should return 401 when no JWT token provided")
    void shouldReturn401_whenNoJwtToken() throws Exception {
        mockMvc.perform(
                        get("/api/v1/finance/invoices")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(8)
    @DisplayName("Should return 400 when creating invoice with empty customer name")
    void shouldReturn400_whenCustomerNameIsEmpty() throws Exception {
        CreateInvoiceRequest invalidRequest = CreateInvoiceRequest.builder()
                .customerName("")  // invalid
                .customerId("cust-001")
                .currency("EUR")
                .taxRate(new BigDecimal("20.00"))
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .lines(List.of())
                .build();

        mockMvc.perform(
                        post("/api/v1/finance/invoices")
                                .with(jwt()
                                        .jwt(jwt -> jwt
                                                .claim("tenantId", "fr-integration")
                                                .claim("realm_access",
                                                        java.util.Map.of("roles", List.of("FINANCE_USER")))
                                        ))
                                .header("X-Tenant-ID", "fr-integration")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static CreateInvoiceRequest buildCreateRequest() {
        return CreateInvoiceRequest.builder()
                .customerName("Integration Test SA")
                .customerId("cust-integration-001")
                .customerEmail("contact@integration-test.fr")
                .currency("EUR")
                .taxRate(new BigDecimal("20.00"))
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .lines(List.of(
                        CreateInvoiceRequest.LineRequest.builder()
                                .description("Test service — integration")
                                .quantity(new BigDecimal("2"))
                                .unitPrice(new BigDecimal("750.00"))
                                .discountPercent(BigDecimal.ZERO)
                                .taxRate(new BigDecimal("20.00"))
                                .build()
                ))
                .build();
    }

    private void assumeCreatedInvoiceExists() {
        Assumptions.assumeTrue(
                createdInvoiceId != null && !createdInvoiceId.isEmpty(),
                "Invoice must have been created by test @Order(1)"
        );
    }
}
