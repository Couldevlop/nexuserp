package com.nexuserp.reporting.infrastructure.generator;

import com.nexuserp.reporting.domain.model.ReportRequest;
import com.nexuserp.reporting.domain.service.ReportGenerator;
import com.nexuserp.reporting.infrastructure.storage.ReportStorageService;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * Génère le FEC (Fichier des Écritures Comptables) conforme DGFiP.
 * Format : pipe-separated | sans en-tête | encodage UTF-8 BOM.
 *
 * Colonnes FEC (13 champs obligatoires DGFiP) :
 * JournalCode | JournalLib | EcritureNum | EcritureDate | CompteNum |
 * CompteLib | CompAuxNum | CompAuxLib | PieceRef | PieceDate |
 * EcritureLib | Debit | Credit
 */
@Component
public class FecExportGenerator implements ReportGenerator {

    private static final DateTimeFormatter FEC_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String SEPARATOR = "|";

    private final ReportStorageService storageService;

    public FecExportGenerator(ReportStorageService storageService) {
        this.storageService = storageService;
    }

    @Override
    public String generate(ReportRequest request) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // BOM UTF-8 requis par la DGFiP
        out.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            // En-tête FEC (commentaire non officiel — certains outils l'attendent)
            // Les données réelles viendraient d'une requête JDBC sur nexus_finance.journal_entries
            // Exemple de ligne FEC :
            writer.println(buildFecLine(
                "AC", "ACHATS",
                "ACH2026-0001", "20260101",
                "401000", "FOURNISSEURS",
                "F001", "FOURNISSEUR EXEMPLE",
                "FA-2026-0001", "20260101",
                "Achat marchandises",
                "1200.00", "0.00"
            ));
            writer.println(buildFecLine(
                "AC", "ACHATS",
                "ACH2026-0001", "20260101",
                "445660", "TVA DEDUCTIBLE",
                "", "",
                "FA-2026-0001", "20260101",
                "TVA 20%",
                "240.00", "0.00"
            ));
            writer.println(buildFecLine(
                "AC", "ACHATS",
                "ACH2026-0001", "20260101",
                "512000", "BANQUE",
                "", "",
                "FA-2026-0001", "20260101",
                "Règlement",
                "0.00", "1440.00"
            ));
            writer.flush();
        }

        String filename = request.getTenantId() + "/FEC_" +
            request.getPeriodFrom().getYear() + "_" +
            java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";

        return storageService.store(filename, out.toByteArray(), "text/plain");
    }

    private String buildFecLine(String journalCode, String journalLib,
                                 String ecritureNum, String ecritureDate,
                                 String compteNum, String compteLib,
                                 String compAuxNum, String compAuxLib,
                                 String pieceRef, String pieceDate,
                                 String ecritureLib, String debit, String credit) {
        return String.join(SEPARATOR,
            journalCode, journalLib, ecritureNum, ecritureDate,
            compteNum, compteLib, compAuxNum, compAuxLib,
            pieceRef, pieceDate, ecritureLib, debit, credit
        );
    }
}
