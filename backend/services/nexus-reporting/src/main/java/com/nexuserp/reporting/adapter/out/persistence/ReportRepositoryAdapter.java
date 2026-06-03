package com.nexuserp.reporting.adapter.out.persistence;

import com.nexuserp.reporting.domain.model.ReportRequest;
import com.nexuserp.reporting.domain.port.out.ReportRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ReportRepositoryAdapter implements ReportRepository {

    private final ReportJpaRepository jpaRepository;

    public ReportRepositoryAdapter(ReportJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ReportRequest save(ReportRequest report) {
        ReportJpaEntity e = toEntity(report);
        ReportJpaEntity saved = jpaRepository.save(e);
        return toDomain(saved);
    }

    @Override
    public Optional<ReportRequest> findById(String id, String tenantId) {
        return jpaRepository.findByIdAndTenantId(id, tenantId).map(this::toDomain);
    }

    private ReportJpaEntity toEntity(ReportRequest r) {
        ReportJpaEntity e = new ReportJpaEntity();
        e.setId(r.getId());
        e.setTenantId(r.getTenantId());
        e.setRequestedBy(r.getRequestedBy());
        e.setType(r.getType().name());
        e.setPeriodFrom(r.getPeriodFrom());
        e.setPeriodTo(r.getPeriodTo());
        e.setOutputFormat(r.getOutputFormat() != null ? r.getOutputFormat().name() : "XLSX");
        e.setStatus(r.getStatus() != null ? r.getStatus().name() : "PENDING");
        e.setDownloadUrl(r.getDownloadUrl());
        e.setErrorMessage(r.getErrorMessage());
        e.setRequestedAt(r.getRequestedAt());
        e.setCompletedAt(r.getCompletedAt());
        return e;
    }

    private ReportRequest toDomain(ReportJpaEntity e) {
        ReportRequest r = ReportRequest.builder()
            .id(e.getId())
            .tenantId(e.getTenantId())
            .requestedBy(e.getRequestedBy())
            .type(ReportRequest.ReportType.valueOf(e.getType()))
            .periodFrom(e.getPeriodFrom())
            .periodTo(e.getPeriodTo())
            .outputFormat(e.getOutputFormat() != null
                ? ReportRequest.OutputFormat.valueOf(e.getOutputFormat())
                : ReportRequest.OutputFormat.XLSX)
            .requestedAt(e.getRequestedAt())
            .build();
        if ("COMPLETED".equals(e.getStatus())) r.markCompleted(e.getDownloadUrl());
        else if ("FAILED".equals(e.getStatus())) r.markFailed(e.getErrorMessage());
        else if ("PROCESSING".equals(e.getStatus())) r.markProcessing();
        return r;
    }
}
