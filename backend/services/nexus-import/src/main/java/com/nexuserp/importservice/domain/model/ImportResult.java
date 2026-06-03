package com.nexuserp.importservice.domain.model;

import java.util.List;

/**
 * Résultat d'un import XLSX — retourné à l'utilisateur avec détails ligne par ligne.
 */
public record ImportResult<T>(
    int totalRows,
    int successRows,
    int errorRows,
    List<ImportError> errors,
    List<T> importedEntities,
    String downloadableErrorReport   // URL MinIO du rapport d'erreurs colorisé
) {
    public record ImportError(
        int rowNumber,
        String column,
        Object rejectedValue,
        String message
    ) {}

    public boolean hasErrors() {
        return errorRows > 0;
    }

    public static <T> ImportResult<T> success(List<T> entities) {
        return new ImportResult<>(entities.size(), entities.size(), 0, List.of(), entities, null);
    }

    public static <T> ImportResult<T> withErrors(List<ImportError> errors) {
        return new ImportResult<>(errors.size(), 0, errors.size(), errors, List.of(), null);
    }
}
