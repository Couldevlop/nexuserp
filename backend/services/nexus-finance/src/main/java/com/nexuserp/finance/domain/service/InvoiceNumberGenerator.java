package com.nexuserp.finance.domain.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Génère des numéros de facture séquentiels par tenant/type/année.
 * Utilise Redis pour garantir l'unicité cross-instance.
 * Format : FA-2026-ACME-000123 (CUSTOMER)
 *          FA-S-2026-000045    (SUPPLIER)
 */
@Component
public class InvoiceNumberGenerator {

    private static final DateTimeFormatter YEAR_FMT = DateTimeFormatter.ofPattern("yyyy");
    private final StringRedisTemplate redisTemplate;

    public InvoiceNumberGenerator(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String generate(String tenantId, String invoiceType) {
        String year = YEAR_FMT.format(LocalDate.now());
        String prefix = getPrefix(invoiceType);
        String key = "invoice:seq:" + tenantId + ":" + prefix + ":" + year;

        Long seq = redisTemplate.opsForValue().increment(key);
        return String.format("%s-%s-%s-%06d", prefix, year, tenantId.toUpperCase(), seq);
    }

    private String getPrefix(String invoiceType) {
        return switch (invoiceType) {
            case "CUSTOMER"    -> "FA";
            case "SUPPLIER"    -> "FF";
            case "CREDIT_NOTE" -> "AV";
            case "DEBIT_NOTE"  -> "DN";
            default            -> "FA";
        };
    }
}
