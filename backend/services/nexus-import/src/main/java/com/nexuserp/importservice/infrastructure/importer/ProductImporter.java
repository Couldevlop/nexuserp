package com.nexuserp.importservice.infrastructure.importer;

import com.nexuserp.importservice.domain.model.ImportResult;
import com.nexuserp.importservice.infrastructure.kafka.ImportEventPublisher;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Import d'articles / produits depuis XLSX.
 * Colonnes attendues (index 0-based):
 *   0: code (obligatoire)
 *   1: name (obligatoire)
 *   2: description
 *   3: unit (UNIT, KG, L, M, BOX)
 *   4: unitPrice
 *   5: reorderPoint
 *   6: category
 */
@Component
public class ProductImporter extends AbstractExcelImporter<Map<String, Object>> {

    private static final int COL_CODE = 0;
    private static final int COL_NAME = 1;
    private static final int COL_DESCRIPTION = 2;
    private static final int COL_UNIT = 3;
    private static final int COL_PRICE = 4;
    private static final int COL_REORDER = 5;
    private static final int COL_CATEGORY = 6;

    private final ImportEventPublisher eventPublisher;
    private final RestTemplate restTemplate;
    private final String inventoryServiceUrl;

    public ProductImporter(ImportEventPublisher eventPublisher, RestTemplate restTemplate,
                           @org.springframework.beans.factory.annotation.Value("${nexuserp.services.inventory-url:http://nexus-inventory:8084}") String inventoryServiceUrl) {
        this.eventPublisher = eventPublisher;
        this.restTemplate = restTemplate;
        this.inventoryServiceUrl = inventoryServiceUrl;
    }

    @Override
    protected List<ImportResult.ImportError> validate(List<Row> rows, String tenantId) {
        List<ImportResult.ImportError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int rowNum = i + 3; // +3 because data starts at row 3 (0-indexed: 2)

            String code = getCellStringValue(row, COL_CODE);
            if (code == null || code.isBlank()) {
                errors.add(new ImportResult.ImportError(rowNum, "code", null, "Le code article est obligatoire"));
            }

            String name = getCellStringValue(row, COL_NAME);
            if (name == null || name.isBlank()) {
                errors.add(new ImportResult.ImportError(rowNum, "name", null, "Le nom de l'article est obligatoire"));
            }

            String unit = getCellStringValue(row, COL_UNIT);
            if (unit != null && !unit.isBlank()) {
                List<String> validUnits = List.of("UNIT", "KG", "L", "M", "BOX", "PIECE", "SET");
                if (!validUnits.contains(unit.toUpperCase())) {
                    errors.add(new ImportResult.ImportError(rowNum, "unit", unit,
                        "Unité invalide. Valeurs acceptées: " + validUnits));
                }
            }

            Double price = getCellNumericValue(row, COL_PRICE);
            if (price != null && price < 0) {
                errors.add(new ImportResult.ImportError(rowNum, "unitPrice", price, "Le prix ne peut pas être négatif"));
            }
        }
        return errors;
    }

    @Override
    protected List<Map<String, Object>> transform(List<Row> rows, String tenantId) {
        List<Map<String, Object>> products = new ArrayList<>();
        for (Row row : rows) {
            String code = getCellStringValue(row, COL_CODE);
            String name = getCellStringValue(row, COL_NAME);
            String description = getCellStringValue(row, COL_DESCRIPTION);
            String unit = getCellStringValue(row, COL_UNIT);
            Double price = getCellNumericValue(row, COL_PRICE);
            Double reorder = getCellNumericValue(row, COL_REORDER);
            String category = getCellStringValue(row, COL_CATEGORY);

            products.add(Map.of(
                "tenantId", tenantId,
                "code", code != null ? code : "",
                "name", name != null ? name : "",
                "description", description != null ? description : "",
                "unit", unit != null ? unit.toUpperCase() : "UNIT",
                "unitPrice", price != null ? price : 0.0,
                "reorderPoint", reorder != null ? reorder : 0.0,
                "category", category != null ? category : ""
            ));
        }
        return products;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> persist(List<Map<String, Object>> entities, String tenantId) {
        // Appel REST vers nexus-inventory en mode batch
        // En production, on utilise un client Feign ou WebClient sécurisé
        // Pour la démonstration, on retourne directement les entités (import sans appel HTTP)
        return entities;
    }

    @Override
    protected void publishEvents(List<Map<String, Object>> entities, String tenantId) {
        eventPublisher.publishImportComplete(tenantId, "PRODUCTS", entities.size(), entities.size(), 0);
    }
}
