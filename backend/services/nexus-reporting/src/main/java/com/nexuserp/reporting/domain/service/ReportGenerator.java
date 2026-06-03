package com.nexuserp.reporting.domain.service;

import com.nexuserp.reporting.domain.model.ReportRequest;

/**
 * Port — implémenté par chaque générateur de rapport (PDF Jasper, XLSX POI, CSV, etc.)
 */
public interface ReportGenerator {
    String generate(ReportRequest request) throws Exception;
}
