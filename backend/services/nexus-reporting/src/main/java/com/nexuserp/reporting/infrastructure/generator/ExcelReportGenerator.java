package com.nexuserp.reporting.infrastructure.generator;

import com.nexuserp.reporting.domain.model.ReportRequest;
import com.nexuserp.reporting.domain.service.ReportGenerator;
import com.nexuserp.reporting.infrastructure.storage.ReportStorageService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/**
 * Génère des rapports XLSX génériques via Apache POI.
 * Pour chaque type de rapport, construit une feuille avec les colonnes appropriées.
 */
@Component
public class ExcelReportGenerator implements ReportGenerator {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final ReportStorageService storageService;

    public ExcelReportGenerator(ReportStorageService storageService) {
        this.storageService = storageService;
    }

    @Override
    public String generate(ReportRequest request) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(request.getType().name());

            // En-têtes
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = createHeaderStyle(workbook);
            String[] columns = resolveColumns(request.getType());
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            // Métadonnées du rapport
            Row metaRow = sheet.createRow(1);
            metaRow.createCell(0).setCellValue("Tenant: " + request.getTenantId());
            metaRow.createCell(1).setCellValue("Période: " + request.getPeriodFrom() + " → " + request.getPeriodTo());
            metaRow.createCell(2).setCellValue("Généré le: " + java.time.LocalDateTime.now());

            // Note: Les données réelles seraient injectées via des requêtes JPA/JDBC vers la base
            // Cette implémentation génère la structure du rapport; les données proviennent des services domaine
            Row noteRow = sheet.createRow(3);
            noteRow.createCell(0).setCellValue("Rapport généré par NexusERP — " + request.getType());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            String filename = request.getTenantId() + "/" +
                request.getType().name().toLowerCase() + "_" +
                java.time.LocalDateTime.now().format(FMT) + ".xlsx";

            return storageService.store(filename, out.toByteArray(),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        }
    }

    private String[] resolveColumns(ReportRequest.ReportType type) {
        return switch (type) {
            case BALANCE_SHEET     -> new String[]{"Compte", "Libellé", "Débit", "Crédit", "Solde"};
            case INCOME_STATEMENT  -> new String[]{"Compte", "Libellé", "Exercice N", "Exercice N-1", "Variation"};
            case TRIAL_BALANCE     -> new String[]{"Compte", "Libellé", "Débit cumulé", "Crédit cumulé", "Solde débiteur", "Solde créditeur"};
            case GENERAL_LEDGER    -> new String[]{"Date", "N° Pièce", "Journal", "Compte", "Libellé", "Débit", "Crédit"};
            case INVOICE_LIST      -> new String[]{"N° Facture", "Date", "Client/Fournisseur", "Montant HT", "TVA", "Montant TTC", "Statut", "Échéance"};
            case STOCK_VALUATION   -> new String[]{"Référence", "Désignation", "Quantité", "Unité", "PMP", "Valeur totale"};
            case PAYROLL_SUMMARY   -> new String[]{"Matricule", "Nom", "Prénom", "Salaire brut", "Charges patronales", "Charges salariales", "Salaire net"};
            case VAT_DECLARATION   -> new String[]{"Base imposable", "Taux TVA", "TVA collectée", "TVA déductible", "TVA à payer"};
            case AGED_RECEIVABLES  -> new String[]{"Client", "Total", "< 30j", "30-60j", "60-90j", "> 90j"};
            case AGED_PAYABLES     -> new String[]{"Fournisseur", "Total", "< 30j", "30-60j", "60-90j", "> 90j"};
            case BUDGET_VARIANCE   -> new String[]{"Centre", "Budget", "Réalisé", "Écart", "Écart %"};
            default                -> new String[]{"Colonne 1", "Colonne 2", "Colonne 3"};
        };
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font fontWhite = wb.createFont();
        fontWhite.setBold(true);
        fontWhite.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(fontWhite);
        return style;
    }
}
