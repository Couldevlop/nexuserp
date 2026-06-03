package com.nexuserp.reporting.domain.port.in;

import com.nexuserp.reporting.domain.model.ReportRequest;

public interface GenerateReportUseCase {
    ReportRequest generate(ReportRequest request);
    ReportRequest getStatus(String reportId, String tenantId);
}
