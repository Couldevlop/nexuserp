package com.nexuserp.notification.domain.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notification_logs", schema = "nexus_notification")
public class NotificationLog {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationMessage.NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    public enum Status { SUCCESS, FAILED }

    public NotificationLog() {}

    public NotificationLog(String id, String tenantId, String recipientEmail,
                           NotificationMessage.NotificationType type, Status status,
                           String errorMessage, Instant sentAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.recipientEmail = recipientEmail;
        this.type = type;
        this.status = status;
        this.errorMessage = errorMessage;
        this.sentAt = sentAt;
    }

    public String getId() { return id; }
    public String getTenantId() { return tenantId; }
    public String getRecipientEmail() { return recipientEmail; }
    public NotificationMessage.NotificationType getType() { return type; }
    public Status getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getSentAt() { return sentAt; }
}
