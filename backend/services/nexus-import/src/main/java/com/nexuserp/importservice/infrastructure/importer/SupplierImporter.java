package com.nexuserp.importservice.infrastructure.importer;

import com.nexuserp.importservice.domain.model.ImportResult;
import com.nexuserp.importservice.infrastructure.kafka.ImportEventPublisher;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Import de fournisseurs depuis XLSX.
 * Colonnes (0-based):
 *   0: code (obligatoire)
 *   1: name (obligatoire)
 *   2: email
 *   3: phone
 *   4: address
 *   5: city
 *   6: country (FR, CI, ...)
 *   7: taxId (SIRET ou registre du commerce)
 *   8: paymentTermsDays
 */
@Component
public class SupplierImporter extends AbstractExcelImporter<Map<String, Object>> {

    private final ImportEventPublisher eventPublisher;

    public SupplierImporter(ImportEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    protected List<ImportResult.ImportError> validate(List<Row> rows, String tenantId) {
        List<ImportResult.ImportError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int rowNum = i + 3;

            String code = getCellStringValue(row, 0);
            if (code == null || code.isBlank()) {
                errors.add(new ImportResult.ImportError(rowNum, "code", null, "Le code fournisseur est obligatoire"));
            }

            String name = getCellStringValue(row, 1);
            if (name == null || name.isBlank()) {
                errors.add(new ImportResult.ImportError(rowNum, "name", null, "Le nom du fournisseur est obligatoire"));
            }

            String email = getCellStringValue(row, 2);
            if (email != null && !email.isBlank() && !email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
                errors.add(new ImportResult.ImportError(rowNum, "email", email, "Format d'email invalide"));
            }

            Double paymentDays = getCellNumericValue(row, 8);
            if (paymentDays != null && (paymentDays < 0 || paymentDays > 365)) {
                errors.add(new ImportResult.ImportError(rowNum, "paymentTermsDays", paymentDays,
                    "Délai de paiement invalide (0-365 jours)"));
            }
        }
        return errors;
    }

    @Override
    protected List<Map<String, Object>> transform(List<Row> rows, String tenantId) {
        List<Map<String, Object>> suppliers = new ArrayList<>();
        for (Row row : rows) {
            suppliers.add(Map.of(
                "tenantId", tenantId,
                "code", safeStr(getCellStringValue(row, 0)),
                "name", safeStr(getCellStringValue(row, 1)),
                "email", safeStr(getCellStringValue(row, 2)),
                "phone", safeStr(getCellStringValue(row, 3)),
                "address", safeStr(getCellStringValue(row, 4)),
                "city", safeStr(getCellStringValue(row, 5)),
                "country", safeStr(getCellStringValue(row, 6)),
                "taxId", safeStr(getCellStringValue(row, 7)),
                "paymentTermsDays", getCellNumericValue(row, 8) != null ? getCellNumericValue(row, 8).intValue() : 30
            ));
        }
        return suppliers;
    }

    @Override
    protected List<Map<String, Object>> persist(List<Map<String, Object>> entities, String tenantId) {
        return entities;
    }

    @Override
    protected void publishEvents(List<Map<String, Object>> entities, String tenantId) {
        eventPublisher.publishImportComplete(tenantId, "SUPPLIERS", entities.size(), entities.size(), 0);
    }

    private String safeStr(String s) { return s != null ? s : ""; }
}
