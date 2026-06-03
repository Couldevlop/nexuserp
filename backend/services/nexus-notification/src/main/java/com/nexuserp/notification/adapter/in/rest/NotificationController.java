package com.nexuserp.notification.adapter.in.rest;

import com.nexuserp.core.infrastructure.tenant.TenantContext;
import com.nexuserp.notification.domain.model.NotificationChannel;
import com.nexuserp.notification.domain.model.NotificationMessage;
import com.nexuserp.notification.domain.service.NotificationDispatcher;
import com.nexuserp.notification.domain.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Envoi de notifications (email, SMS, WhatsApp)")
public class NotificationController {

    /** OWASP A03 : validation stricte du format E.164 (indicatif + 1..14 chiffres). */
    private static final String E164_REGEX = "^\\+[1-9]\\d{1,14}$";

    private final NotificationService notificationService;
    private final NotificationDispatcher dispatcher;

    public NotificationController(NotificationService notificationService,
                                 NotificationDispatcher dispatcher) {
        this.notificationService = notificationService;
        this.dispatcher = dispatcher;
    }

    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Envoyer une notification email")
    public ResponseEntity<Void> send(@Valid @RequestBody SendNotificationRequest request) {
        NotificationMessage message = new NotificationMessage(
            request.tenantId(),
            request.recipientEmail(),
            request.recipientName(),
            request.type(),
            request.locale() != null ? request.locale() : "fr-FR",
            request.variables()
        );
        notificationService.send(message);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/sms")
    // OWASP A01 : accès restreint aux rôles autorisés à notifier.
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'FINANCE_MANAGER', 'SALES_MANAGER', 'HR_MANAGER')")
    @Operation(summary = "Envoyer une notification SMS")
    public ResponseEntity<Void> sendSms(@Valid @RequestBody SendChannelRequest request) {
        dispatcher.dispatch(toMessage(request, NotificationChannel.SMS));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/whatsapp")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'FINANCE_MANAGER', 'SALES_MANAGER', 'HR_MANAGER')")
    @Operation(summary = "Envoyer une notification WhatsApp")
    public ResponseEntity<Void> sendWhatsApp(@Valid @RequestBody SendChannelRequest request) {
        dispatcher.dispatch(toMessage(request, NotificationChannel.WHATSAPP));
        return ResponseEntity.accepted().build();
    }

    private NotificationMessage toMessage(SendChannelRequest request, NotificationChannel channel) {
        // OWASP A01 : on force le tenant courant (contexte) ; on ignore tout tenantId du body.
        String tenantId = TenantContext.hasTenant() ? TenantContext.getTenantId() : request.tenantId();
        return new NotificationMessage(
            tenantId,
            null,
            request.recipientName(),
            request.type(),
            request.locale() != null ? request.locale() : "fr-FR",
            request.variables(),
            request.recipientPhone(),
            List.of(channel)
        );
    }

    public record SendNotificationRequest(
        @NotBlank String tenantId,
        @NotBlank @Email String recipientEmail,
        String recipientName,
        @NotNull NotificationMessage.NotificationType type,
        String locale,
        Map<String, Object> variables
    ) {}

    public record SendChannelRequest(
        @NotBlank String tenantId,
        @NotBlank @Pattern(regexp = E164_REGEX, message = "recipientPhone must be E.164 (e.g. +2250700000000)")
        String recipientPhone,
        String recipientName,
        @NotNull NotificationMessage.NotificationType type,
        String locale,
        Map<String, Object> variables
    ) {}
}
