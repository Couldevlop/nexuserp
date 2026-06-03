package com.nexuserp.importservice.infrastructure.importer;

import com.nexuserp.importservice.domain.model.ImportResult;
import com.nexuserp.importservice.infrastructure.kafka.ImportEventPublisher;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Import de salariés depuis XLSX.
 * Colonnes (0-based):
 *   0:  employeeNumber (obligatoire)
 *   1:  firstName (obligatoire)
 *   2:  lastName (obligatoire)
 *   3:  email (obligatoire)
 *   4:  phone
 *   5:  department
 *   6:  position
 *   7:  contractType (CDI, CDD, INTERIM, STAGE)
 *   8:  hireDate (yyyy-MM-dd)
 *   9:  baseSalary
 *   10: country (FR, CI)
 */
@Component
public class EmployeeImporter extends AbstractExcelImporter<Map<String, Object>> {

    private final ImportEventPublisher eventPublisher;

    public EmployeeImporter(ImportEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    protected List<ImportResult.ImportError> validate(List<Row> rows, String tenantId) {
        List<ImportResult.ImportError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int rowNum = i + 3;

            String number = getCellStringValue(row, 0);
            if (number == null || number.isBlank()) {
                errors.add(new ImportResult.ImportError(rowNum, "employeeNumber", null, "Le matricule est obligatoire"));
            }

            String firstName = getCellStringValue(row, 1);
            if (firstName == null || firstName.isBlank()) {
                errors.add(new ImportResult.ImportError(rowNum, "firstName", null, "Le prénom est obligatoire"));
            }

            String lastName = getCellStringValue(row, 2);
            if (lastName == null || lastName.isBlank()) {
                errors.add(new ImportResult.ImportError(rowNum, "lastName", null, "Le nom est obligatoire"));
            }

            String email = getCellStringValue(row, 3);
            if (email == null || email.isBlank()) {
                errors.add(new ImportResult.ImportError(rowNum, "email", null, "L'email est obligatoire"));
            } else if (!email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
                errors.add(new ImportResult.ImportError(rowNum, "email", email, "Format d'email invalide"));
            }

            String contractType = getCellStringValue(row, 7);
            if (contractType != null && !contractType.isBlank()) {
                List<String> validTypes = List.of("CDI", "CDD", "INTERIM", "STAGE", "APPRENTISSAGE");
                if (!validTypes.contains(contractType.toUpperCase())) {
                    errors.add(new ImportResult.ImportError(rowNum, "contractType", contractType,
                        "Type de contrat invalide. Valeurs: " + validTypes));
                }
            }

            Double salary = getCellNumericValue(row, 9);
            if (salary != null && salary < 0) {
                errors.add(new ImportResult.ImportError(rowNum, "baseSalary", salary, "Le salaire ne peut pas être négatif"));
            }
        }
        return errors;
    }

    @Override
    protected List<Map<String, Object>> transform(List<Row> rows, String tenantId) {
        List<Map<String, Object>> employees = new ArrayList<>();
        for (Row row : rows) {
            Map<String, Object> entry = new java.util.LinkedHashMap<>();
            entry.put("tenantId", tenantId);
            entry.put("employeeNumber", safeStr(getCellStringValue(row, 0)));
            entry.put("firstName", safeStr(getCellStringValue(row, 1)));
            entry.put("lastName", safeStr(getCellStringValue(row, 2)));
            entry.put("email", safeStr(getCellStringValue(row, 3)));
            entry.put("phone", safeStr(getCellStringValue(row, 4)));
            entry.put("department", safeStr(getCellStringValue(row, 5)));
            entry.put("position", safeStr(getCellStringValue(row, 6)));
            entry.put("contractType", safeStr(getCellStringValue(row, 7)));
            entry.put("hireDate", safeStr(getCellStringValue(row, 8)));
            entry.put("baseSalary", getCellNumericValue(row, 9) != null ? getCellNumericValue(row, 9) : 0.0);
            entry.put("country", safeStr(getCellStringValue(row, 10)));
            employees.add(entry);
        }
        return employees;
    }

    @Override
    protected List<Map<String, Object>> persist(List<Map<String, Object>> entities, String tenantId) {
        return entities;
    }

    @Override
    protected void publishEvents(List<Map<String, Object>> entities, String tenantId) {
        eventPublisher.publishImportComplete(tenantId, "EMPLOYEES", entities.size(), entities.size(), 0);
    }

    private String safeStr(String s) { return s != null ? s : ""; }
}
