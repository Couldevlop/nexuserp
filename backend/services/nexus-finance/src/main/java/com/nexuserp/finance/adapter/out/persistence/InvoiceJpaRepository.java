package com.nexuserp.finance.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceJpaRepository extends JpaRepository<InvoiceJpaEntity, UUID> {

    Optional<InvoiceJpaEntity> findByIdAndTenantId(UUID id, String tenantId);

    Page<InvoiceJpaEntity> findByTenantIdAndStatus(String tenantId, String status, Pageable pageable);

    Page<InvoiceJpaEntity> findByTenantId(String tenantId, Pageable pageable);

    boolean existsByInvoiceNumberAndTenantId(String invoiceNumber, String tenantId);

    @Query("""
        SELECT i FROM InvoiceJpaEntity i
        WHERE i.tenantId = :tenantId
          AND i.status = 'APPROVED'
          AND i.dueDate < :beforeDate
        """)
    List<InvoiceJpaEntity> findOverdueInvoices(
        @Param("tenantId") String tenantId,
        @Param("beforeDate") LocalDate beforeDate);
}
