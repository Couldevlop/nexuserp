package com.nexuserp.reporting.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * ReportRequest — value object describing what to generate.
 * Supported types: BALANCE_SHEET, INCOME_STATEMENT, TRIAL_BALANCE,
 * CASH_FLOW, FEC_EXPORT, SYSCOHADA_EXPORT, GENERAL_LEDGER,
 * INVOICE_LIST, STOCK_VALUATION, PAYROLL_SUMMARY
 */
@Getter
@Builder
public class ReportRequest {

    public enum OutputFormat { PDF, XLSX, CSV, JSON }

    public enum Status { PENDING, PROCESSING, COMPLETED, FAILED }

    private final String id;
    private final String tenantId;
    private final String requestedBy;
    private final ReportType type;
    private final LocalDate periodFrom;
    private final LocalDate periodTo;
    private final OutputFormat outputFormat;
    private final Map<String, String> parameters;
    private final LocalDateTime requestedAt;

    private Status status;
    private String downloadUrl;
    private String errorMessage;
    private LocalDateTime completedAt;

    public void markProcessing() {
        this.status = Status.PROCESSING;
    }

    public void markCompleted(String url) {
        Objects.requireNonNull(url, "Download URL required");
        this.downloadUrl = url;
        this.status = Status.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void markFailed(String error) {
        this.errorMessage = error;
        this.status = Status.FAILED;
        this.completedAt = LocalDateTime.now();
    }

    public enum ReportType {
        BALANCE_SHEET,        // Bilan
        INCOME_STATEMENT,     // Compte de résultat
        TRIAL_BALANCE,        // Balance générale
        CASH_FLOW,            // Tableau flux trésorerie
        GENERAL_LEDGER,       // Grand livre
        FEC_EXPORT,           // Fichier des Écritures Comptables (France)
        SYSCOHADA_EXPORT,     // États de synthèse SYSCOHADA (CI/UEMOA)
        INVOICE_LIST,         // Liste des factures
        STOCK_VALUATION,      // Valorisation stocks
        PAYROLL_SUMMARY,      // Récapitulatif paie
        VAT_DECLARATION,      // Déclaration TVA (CA3/CA12)
        AGED_RECEIVABLES,     // Balance âgée clients
        AGED_PAYABLES,        // Balance âgée fournisseurs
        BUDGET_VARIANCE,      // Réalisé vs Budget
        KPI_DASHBOARD         // KPI synthétiques
    }
}
