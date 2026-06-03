package com.nexuserp.importservice.infrastructure.importer;

import com.nexuserp.importservice.domain.model.ImportResult;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Template Method Pattern — Base pour tous les importeurs XLSX.
 * Sous-classes implementent : validate, transform, persist.
 */
public abstract class AbstractExcelImporter<T> {

    private static final Logger log = LoggerFactory.getLogger(AbstractExcelImporter.class);
    private static final long MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024; // 50 MB
    private static final int BATCH_SIZE = 500;

    public final ImportResult<T> process(MultipartFile file, String tenantId) {
        log.info("Starting import: file={}, size={}, tenant={}",
            file.getOriginalFilename(), file.getSize(), tenantId);

        // 1. Validation du fichier
        List<ImportResult.ImportError> fileErrors = validateFile(file);
        if (!fileErrors.isEmpty()) {
            return ImportResult.withErrors(fileErrors);
        }

        // 2. Parsing XLSX
        List<Row> rows;
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            rows = new ArrayList<>();
            // Ligne 0 = en-têtes, Ligne 1 = exemple (à ignorer), données à partir de ligne 2
            for (int i = 2; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null && !isRowEmpty(row)) {
                    rows.add(row);
                }
            }
        } catch (IOException e) {
            log.error("Failed to parse XLSX file: {}", e.getMessage());
            return ImportResult.withErrors(List.of(
                new ImportResult.ImportError(0, "file", file.getOriginalFilename(), "Impossible de lire le fichier: " + e.getMessage())
            ));
        }

        if (rows.isEmpty()) {
            return ImportResult.withErrors(List.of(
                new ImportResult.ImportError(0, "file", null, "Le fichier ne contient aucune donnée (les 2 premières lignes sont réservées aux en-têtes et exemples)")
            ));
        }

        // 3. Validation des données
        List<ImportResult.ImportError> dataErrors = validate(rows, tenantId);
        if (!dataErrors.isEmpty()) {
            log.warn("Import validation failed: {} errors", dataErrors.size());
            return ImportResult.withErrors(dataErrors);
        }

        // 4. Transformation
        List<T> entities = transform(rows, tenantId);

        // 5. Persistance par batch
        List<T> persisted = new ArrayList<>();
        for (int i = 0; i < entities.size(); i += BATCH_SIZE) {
            List<T> batch = entities.subList(i, Math.min(i + BATCH_SIZE, entities.size()));
            persisted.addAll(persist(batch, tenantId));
        }

        // 6. Publication événements
        publishEvents(persisted, tenantId);

        log.info("Import complete: {} rows processed, {} entities created, tenant={}",
            rows.size(), persisted.size(), tenantId);

        return ImportResult.success(persisted);
    }

    protected List<ImportResult.ImportError> validateFile(MultipartFile file) {
        List<ImportResult.ImportError> errors = new ArrayList<>();
        if (file.isEmpty()) {
            errors.add(new ImportResult.ImportError(0, "file", null, "Le fichier est vide"));
            return errors;
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            errors.add(new ImportResult.ImportError(0, "file", file.getSize(),
                "Fichier trop volumineux (max 50 MB)"));
        }
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            errors.add(new ImportResult.ImportError(0, "file", filename,
                "Seuls les fichiers .xlsx et .xls sont acceptés"));
        }
        return errors;
    }

    protected boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    protected String getCellStringValue(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCachedFormulaResultType() == CellType.STRING
                ? cell.getStringCellValue() : String.valueOf(cell.getNumericCellValue());
            default -> null;
        };
    }

    protected Double getCellNumericValue(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING  -> {
                try { yield Double.parseDouble(cell.getStringCellValue().trim()); }
                catch (NumberFormatException e) { yield null; }
            }
            default -> null;
        };
    }

    // Hook methods — à implémenter par les sous-classes
    protected abstract List<ImportResult.ImportError> validate(List<Row> rows, String tenantId);
    protected abstract List<T> transform(List<Row> rows, String tenantId);
    protected abstract List<T> persist(List<T> entities, String tenantId);
    protected void publishEvents(List<T> entities, String tenantId) {}
}
