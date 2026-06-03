package com.nexuserp.reporting.domain.service;

import com.nexuserp.reporting.domain.model.ReportRequest;
import com.nexuserp.reporting.infrastructure.generator.ExcelReportGenerator;
import com.nexuserp.reporting.infrastructure.generator.FecExportGenerator;
import org.springframework.stereotype.Component;

@Component
public class ReportGeneratorFactory {

    private final ExcelReportGenerator excelGenerator;
    private final FecExportGenerator fecGenerator;

    public ReportGeneratorFactory(ExcelReportGenerator excelGenerator, FecExportGenerator fecGenerator) {
        this.excelGenerator = excelGenerator;
        this.fecGenerator = fecGenerator;
    }

    public ReportGenerator get(ReportRequest.ReportType type) {
        return switch (type) {
            case FEC_EXPORT       -> fecGenerator;
            default               -> excelGenerator;
        };
    }
}
