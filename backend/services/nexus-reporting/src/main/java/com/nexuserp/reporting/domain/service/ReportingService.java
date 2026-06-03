package com.nexuserp.reporting.domain.service;

import com.nexuserp.reporting.domain.model.ReportRequest;
import com.nexuserp.reporting.domain.port.in.GenerateReportUseCase;
import com.nexuserp.reporting.domain.port.out.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ReportingService implements GenerateReportUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReportingService.class);

    private final ReportRepository reportRepository;
    private final ReportGeneratorFactory generatorFactory;

    public ReportingService(ReportRepository reportRepository, ReportGeneratorFactory generatorFactory) {
        this.reportRepository = reportRepository;
        this.generatorFactory = generatorFactory;
    }

    @Override
    public ReportRequest generate(ReportRequest request) {
        ReportRequest saved = reportRepository.save(
            ReportRequest.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(request.getTenantId())
                .requestedBy(request.getRequestedBy())
                .type(request.getType())
                .periodFrom(request.getPeriodFrom())
                .periodTo(request.getPeriodTo())
                .outputFormat(request.getOutputFormat())
                .parameters(request.getParameters())
                .requestedAt(java.time.LocalDateTime.now())
                .build()
        );
        saved.markProcessing();
        reportRepository.save(saved);
        generateAsync(saved);
        return saved;
    }

    @Async
    public void generateAsync(ReportRequest request) {
        try {
            log.info("Generating report: id={}, type={}, tenant={}", request.getId(), request.getType(), request.getTenantId());
            ReportGenerator generator = generatorFactory.get(request.getType());
            String downloadUrl = generator.generate(request);
            request.markCompleted(downloadUrl);
            reportRepository.save(request);
            log.info("Report generated: id={}, url={}", request.getId(), downloadUrl);
        } catch (Exception e) {
            log.error("Report generation failed: id={}, error={}", request.getId(), e.getMessage(), e);
            request.markFailed(e.getMessage());
            reportRepository.save(request);
        }
    }

    @Override
    public ReportRequest getStatus(String reportId, String tenantId) {
        return reportRepository.findById(reportId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
    }
}
