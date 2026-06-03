package com.nexuserp.core.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("TenantId Value Object")
class TenantIdTest {

    @Test
    @DisplayName("Should create TenantId with valid value")
    void shouldCreate_whenValidValue() {
        TenantId tenantId = TenantId.of("acme-corp");
        assertThat(tenantId.value()).isEqualTo("acme-corp");
    }

    @ParameterizedTest
    @ValueSource(strings = {"acme", "my-tenant-001", "tenant123", "a1b2c3"})
    @DisplayName("Should accept valid tenant IDs")
    void shouldAccept_validTenantIds(String value) {
        assertThatNoException().isThrownBy(() -> TenantId.of(value));
    }

    @Test
    @DisplayName("Should throw when value is null")
    void shouldThrow_whenNull() {
        assertThatThrownBy(() -> TenantId.of(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw when value is empty")
    void shouldThrow_whenEmpty() {
        assertThatThrownBy(() -> TenantId.of(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"AB", "UPPERCASE", "tenant name", "tenant.name", "-start", "end-", "a"})
    @DisplayName("Should throw when value violates regex constraints")
    void shouldThrow_whenInvalidFormat(String value) {
        assertThatThrownBy(() -> TenantId.of(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId");
    }

    @Test
    @DisplayName("Should be equal when values are equal")
    void shouldBeEqual_whenSameValue() {
        TenantId t1 = TenantId.of("acme-corp");
        TenantId t2 = TenantId.of("acme-corp");
        assertThat(t1).isEqualTo(t2);
        assertThat(t1.hashCode()).isEqualTo(t2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when values differ")
    void shouldNotBeEqual_whenDifferentValues() {
        TenantId t1 = TenantId.of("acme-corp");
        TenantId t2 = TenantId.of("other-corp");
        assertThat(t1).isNotEqualTo(t2);
    }

    @Test
    @DisplayName("toString should return the value")
    void shouldReturnValueInToString() {
        assertThat(TenantId.of("acme-corp").toString()).isEqualTo("acme-corp");
    }
}
