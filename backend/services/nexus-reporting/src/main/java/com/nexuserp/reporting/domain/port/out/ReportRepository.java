package com.nexuserp.reporting.domain.port.out;

import com.nexuserp.reporting.domain.model.ReportRequest;

import java.util.Optional;

public interface ReportRepository {
    ReportRequest save(ReportRequest report);
    Optional<ReportRequest> findById(String id, String tenantId);
}
