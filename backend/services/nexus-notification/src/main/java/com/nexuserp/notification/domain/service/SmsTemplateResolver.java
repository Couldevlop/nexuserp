package com.nexuserp.notification.domain.service;

import com.nexuserp.notification.domain.model.NotificationMessage.NotificationType;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Résout le texte court (SMS / WhatsApp) par type de notification + locale (fr / en).
 * Messages volontairement < 160 caractères quand c'est possible.
 *
 * OWASP A03 (injection de template) : seuls les placeholders {nom} référencés dans
 * le gabarit sont substitués, à partir de la map de variables. Les valeurs sont
 * échappées (suppression des sauts de ligne / caractères de contrôle) avant insertion,
 * et la longueur finale est plafonnée. Aucune valeur non maîtrisée n'est concaténée
 * dans une URL/appel fournisseur.
 */
@Service
public class SmsTemplateResolver {

    /** Plafond dur de longueur (concaténation SMS multi-part — protège contre les abus). */
    private static final int MAX_LENGTH = 480;

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-zA-Z0-9_]+)}");

    public String render(NotificationType type, String locale, Map<String, Object> variables) {
        boolean isFr = locale == null || locale.startsWith("fr");
        String template = template(type, isFr);
        String rendered = interpolate(template, variables == null ? Map.of() : variables);
        return cap(rendered);
    }

    private String template(NotificationType type, boolean fr) {
        return switch (type) {
            case TWO_FA_CODE -> fr
                ? "NexusERP: votre code est {code}. Valable 5 min. Ne le partagez jamais."
                : "NexusERP: your code is {code}. Valid 5 min. Never share it.";
            case PASSWORD_RESET -> fr
                ? "NexusERP: demande de reinitialisation de mot de passe. Code: {code}."
                : "NexusERP: password reset requested. Code: {code}.";
            case ACCOUNT_LOCKED -> fr
                ? "NexusERP: votre compte a ete temporairement verrouille pour securite."
                : "NexusERP: your account has been temporarily locked for security.";
            case LOGIN_NEW_DEVICE -> fr
                ? "NexusERP: connexion depuis un nouvel appareil detectee. Si ce n'est pas vous, agissez."
                : "NexusERP: new device login detected. If this wasn't you, take action.";
            case INVOICE_DUE_REMINDER -> fr
                ? "NexusERP: facture {invoiceNumber} de {amount} arrive a echeance le {dueDate}."
                : "NexusERP: invoice {invoiceNumber} of {amount} is due on {dueDate}.";
            case PAYMENT_RECEIVED -> fr
                ? "NexusERP: paiement de {amount} recu. Merci."
                : "NexusERP: payment of {amount} received. Thank you.";
            case BUDGET_EXCEEDED -> fr
                ? "NexusERP: budget {budgetName} depasse ({amount})."
                : "NexusERP: budget {budgetName} exceeded ({amount}).";
            case LOW_STOCK_ALERT -> fr
                ? "NexusERP: stock faible pour {productName} (reste {quantity})."
                : "NexusERP: low stock for {productName} ({quantity} left).";
            case EXPIRY_DATE_ALERT -> fr
                ? "NexusERP: peremption proche pour {productName} le {expiryDate}."
                : "NexusERP: {productName} expires soon on {expiryDate}.";
            case PAYSLIP_AVAILABLE -> fr
                ? "NexusERP: votre bulletin de paie {period} est disponible."
                : "NexusERP: your payslip for {period} is now available.";
            case LEAVE_APPROVED -> fr
                ? "NexusERP: votre demande de conge a ete approuvee."
                : "NexusERP: your leave request has been approved.";
            case DELIVERY_EXPECTED -> fr
                ? "NexusERP: livraison attendue le {date} pour la commande {orderNumber}."
                : "NexusERP: delivery expected on {date} for order {orderNumber}.";
            case PURCHASE_ORDER_APPROVED -> fr
                ? "NexusERP: bon de commande {orderNumber} approuve."
                : "NexusERP: purchase order {orderNumber} approved.";
            case IMPORT_COMPLETE -> fr
                ? "NexusERP: import termine ({successRows} lignes)."
                : "NexusERP: import completed ({successRows} rows).";
            case TENANT_ONBOARDING_COMPLETE -> fr
                ? "NexusERP: bienvenue ! Votre espace est pret."
                : "NexusERP: welcome! Your workspace is ready.";
            case AI_ANOMALY_DETECTED, ACCOUNTING_ANOMALY_DETECTED -> fr
                ? "NexusERP: anomalie detectee, verifiez votre tableau de bord."
                : "NexusERP: anomaly detected, please check your dashboard.";
            default -> fr
                ? "NexusERP: vous avez une nouvelle notification."
                : "NexusERP: you have a new notification.";
        };
    }

    private String interpolate(String template, Map<String, Object> variables) {
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String key = m.group(1);
            Object value = variables.get(key);
            String replacement = value == null ? "" : sanitize(String.valueOf(value));
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** OWASP A03 : neutralise sauts de ligne / caractères de contrôle dans les valeurs injectées. */
    private String sanitize(String value) {
        return value.replaceAll("[\\p{Cntrl}]", " ").trim();
    }

    private String cap(String text) {
        return text.length() > MAX_LENGTH ? text.substring(0, MAX_LENGTH) : text;
    }
}
