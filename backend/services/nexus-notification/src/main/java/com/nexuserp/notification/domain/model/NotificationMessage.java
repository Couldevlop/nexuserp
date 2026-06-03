package com.nexuserp.notification.domain.model;

import java.util.List;
import java.util.Map;

public record NotificationMessage(
    String tenantId,
    String recipientEmail,
    String recipientName,
    NotificationType type,
    String locale,          // fr-FR, fr-CI, en-US
    Map<String, Object> variables,
    String recipientPhone,                 // E.164, requis pour SMS / WHATSAPP
    List<NotificationChannel> channels     // canaux ciblés ; null/vide -> EMAIL par défaut
) {

    /**
     * Constructeur historique (email uniquement) — conservé pour rétro-compatibilité.
     * Tout le flux email existant continue d'appeler celui-ci sans changement.
     */
    public NotificationMessage(
        String tenantId,
        String recipientEmail,
        String recipientName,
        NotificationType type,
        String locale,
        Map<String, Object> variables
    ) {
        this(tenantId, recipientEmail, recipientName, type, locale, variables,
            null, List.of(NotificationChannel.EMAIL));
    }

    /**
     * Canaux effectifs : si aucun canal n'est précisé, on retombe sur EMAIL
     * pour préserver le comportement historique.
     */
    public List<NotificationChannel> effectiveChannels() {
        return (channels == null || channels.isEmpty())
            ? List.of(NotificationChannel.EMAIL)
            : channels;
    }

    public enum NotificationType {
        // Sécurité
        LOGIN_NEW_DEVICE, TWO_FA_CODE, PASSWORD_RESET, ACCOUNT_LOCKED,
        // Finance
        INVOICE_DUE_REMINDER, PAYMENT_RECEIVED, BUDGET_EXCEEDED,
        ACCOUNTING_ANOMALY_DETECTED,
        // Achats
        PURCHASE_ORDER_APPROVED, DELIVERY_EXPECTED, SUPPLIER_INVOICE_RECEIVED,
        // Stocks
        LOW_STOCK_ALERT, EXPIRY_DATE_ALERT,
        // RH
        LEAVE_APPROVED, PAYSLIP_AVAILABLE, CONTRACT_EXPIRY,
        // Système
        IMPORT_COMPLETE, BACKUP_SUCCESS, TENANT_ONBOARDING_COMPLETE,
        // IA
        AI_ANOMALY_DETECTED, AI_FORECAST_READY
    }
}
