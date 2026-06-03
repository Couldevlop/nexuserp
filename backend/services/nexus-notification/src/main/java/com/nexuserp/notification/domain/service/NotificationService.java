package com.nexuserp.notification.domain.service;

import com.nexuserp.notification.domain.model.NotificationMessage;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String fromEmail;
    private final String fromName;

    public NotificationService(JavaMailSender mailSender,
                                TemplateEngine templateEngine,
                                @org.springframework.beans.factory.annotation.Value("${spring.mail.username}") String fromEmail,
                                @org.springframework.beans.factory.annotation.Value("${nexus.notification.from-name:NexusERP}") String fromName) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
    }

    public void send(NotificationMessage message) {
        try {
            String templateName = resolveTemplate(message.type());
            String subject = resolveSubject(message.type(), message.locale());

            Locale locale = resolveLocale(message.locale());
            Context context = new Context(locale);
            context.setVariable("recipientName", message.recipientName());
            context.setVariable("tenantId", message.tenantId());
            if (message.variables() != null) {
                message.variables().forEach(context::setVariable);
            }

            String htmlContent = templateEngine.process("email/" + templateName, context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromEmail, fromName);
            helper.setTo(message.recipientEmail());
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Email sent: type={}, recipient={}, tenant={}",
                message.type(), message.recipientEmail(), message.tenantId());

        } catch (Exception e) {
            log.error("Failed to send email: type={}, recipient={}, error={}",
                message.type(), message.recipientEmail(), e.getMessage(), e);
        }
    }

    private String resolveTemplate(NotificationMessage.NotificationType type) {
        return switch (type) {
            case TWO_FA_CODE           -> "two-fa-code";
            case PASSWORD_RESET        -> "password-reset";
            case ACCOUNT_LOCKED        -> "account-locked";
            case LOGIN_NEW_DEVICE      -> "login-new-device";
            case INVOICE_DUE_REMINDER  -> "invoice-due-reminder";
            case PAYMENT_RECEIVED      -> "payment-received";
            case BUDGET_EXCEEDED       -> "budget-exceeded";
            case ACCOUNTING_ANOMALY_DETECTED -> "anomaly-detected";
            case LOW_STOCK_ALERT       -> "low-stock-alert";
            case LEAVE_APPROVED        -> "leave-approved";
            case PAYSLIP_AVAILABLE     -> "payslip-available";
            case IMPORT_COMPLETE       -> "import-complete";
            case TENANT_ONBOARDING_COMPLETE -> "tenant-onboarding";
            case AI_ANOMALY_DETECTED   -> "ai-anomaly";
            default                    -> "generic";
        };
    }

    private String resolveSubject(NotificationMessage.NotificationType type, String locale) {
        boolean isFr = locale == null || locale.startsWith("fr");
        return switch (type) {
            case TWO_FA_CODE           -> isFr ? "Votre code de vérification NexusERP" : "Your NexusERP verification code";
            case PASSWORD_RESET        -> isFr ? "Réinitialisation de votre mot de passe" : "Password reset request";
            case ACCOUNT_LOCKED        -> isFr ? "Compte temporairement verrouillé" : "Account temporarily locked";
            case LOGIN_NEW_DEVICE      -> isFr ? "Connexion depuis un nouvel appareil" : "New device login detected";
            case INVOICE_DUE_REMINDER  -> isFr ? "Rappel : facture arrivant à échéance" : "Invoice due reminder";
            case PAYMENT_RECEIVED      -> isFr ? "Paiement reçu" : "Payment received";
            case BUDGET_EXCEEDED       -> isFr ? "Alerte : budget dépassé" : "Budget exceeded alert";
            case LOW_STOCK_ALERT       -> isFr ? "Alerte stock faible" : "Low stock alert";
            case IMPORT_COMPLETE       -> isFr ? "Import terminé" : "Import completed";
            case TENANT_ONBOARDING_COMPLETE -> isFr ? "Bienvenue sur NexusERP !" : "Welcome to NexusERP!";
            case AI_ANOMALY_DETECTED   -> isFr ? "[IA] Anomalie comptable détectée" : "[AI] Accounting anomaly detected";
            default                    -> "NexusERP Notification";
        };
    }

    private Locale resolveLocale(String locale) {
        if (locale == null) return Locale.FRENCH;
        return switch (locale) {
            case "fr-FR", "fr-CI" -> Locale.FRENCH;
            case "en-US", "en-GB" -> Locale.ENGLISH;
            default               -> Locale.FRENCH;
        };
    }
}
