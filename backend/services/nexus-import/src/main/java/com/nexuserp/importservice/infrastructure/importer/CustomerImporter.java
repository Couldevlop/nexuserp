package com.nexuserp.importservice.infrastructure.importer;

import com.nexuserp.importservice.domain.model.ImportResult;
import com.nexuserp.importservice.infrastructure.kafka.ImportEventPublisher;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Import de clients depuis XLSX.
 * Colonnes (0-based):
 *   0: code (obligatoire)
 *   1: name (obligatoire)
 *   2: email
 *   3: phone
 *   4: address
 *   5: city
 *   6: country
 *   7: taxId
 *   8: creditLimit
 *   9: paymentTermsDays
 */
@Component
public class CustomerImporter extends AbstractExcelImporter<Map<String, Object>> {

    private final ImportEventPublisher eventPublisher;

    public CustomerImporter(ImportEventPublisher eventPublisher) {
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
                errors.add(new ImportResult.ImportError(rowNum, "code", null, "Le code client est obligatoire"));
            }

            String name = getCellStringValue(row, 1);
            if (name == null || name.isBlank()) {
                errors.add(new ImportResult.ImportError(rowNum, "name", null, "Le nom du client est obligatoire"));
            }

            String email = getCellStringValue(row, 2);
            if (email != null && !email.isBlank() && !email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
                errors.add(new ImportResult.ImportError(rowNum, "email", email, "Format d'email invalide"));
            }

            Double creditLimit = getCellNumericValue(row, 8);
            if (creditLimit != null && creditLimit < 0) {
                errors.add(new ImportResult.ImportError(rowNum, "creditLimit", creditLimit, "La limite de crédit ne peut pas être négative"));
            }
        }
        return errors;
    }

    @Override
    protected List<Map<String, Object>> transform(List<Row> rows, String tenantId) {
        List<Map<String, Object>> customers = new ArrayList<>();
        for (Row row : rows) {
            Map<String, Object> entry = new java.util.LinkedHashMap<>();
            entry.put("tenantId", tenantId);
            entry.put("code", safeStr(getCellStringValue(row, 0)));
            entry.put("name", safeStr(getCellStringValue(row, 1)));
            entry.put("email", safeStr(getCellStringValue(row, 2)));
            entry.put("phone", safeStr(getCellStringValue(row, 3)));
            entry.put("address", safeStr(getCellStringValue(row, 4)));
            entry.put("city", safeStr(getCellStringValue(row, 5)));
            entry.put("country", safeStr(getCellStringValue(row, 6)));
            entry.put("taxId", safeStr(getCellStringValue(row, 7)));
            entry.put("creditLimit", getCellNumericValue(row, 8) != null ? getCellNumericValue(row, 8) : 0.0);
            entry.put("paymentTermsDays", getCellNumericValue(row, 9) != null ? getCellNumericValue(row, 9).intValue() : 30);
            customers.add(entry);
        }
        return customers;
    }

    @Override
    protected List<Map<String, Object>> persist(List<Map<String, Object>> entities, String tenantId) {
        return entities;
    }

    @Override
    protected void publishEvents(List<Map<String, Object>> entities, String tenantId) {
        eventPublisher.publishImportComplete(tenantId, "CUSTOMERS", entities.size(), entities.size(), 0);
    }

    private String safeStr(String s) { return s != null ? s : ""; }
}
