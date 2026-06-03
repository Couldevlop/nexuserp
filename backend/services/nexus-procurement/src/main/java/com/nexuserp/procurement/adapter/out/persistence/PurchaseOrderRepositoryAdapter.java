package com.nexuserp.procurement.adapter.out.persistence;

import com.nexuserp.procurement.domain.model.PurchaseOrder;
import com.nexuserp.procurement.domain.port.out.PurchaseOrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class PurchaseOrderRepositoryAdapter implements PurchaseOrderRepository {

    private final PurchaseOrderJpaRepository jpaRepository;

    public PurchaseOrderRepositoryAdapter(PurchaseOrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PurchaseOrder save(PurchaseOrder order) {
        PurchaseOrderJpaEntity e = new PurchaseOrderJpaEntity();
        e.setId(order.getId());
        e.setTenantId(order.getTenantId());
        e.setPoNumber(order.getOrderNumber());
        e.setSupplierId(order.getSupplierId());
        e.setSupplierName(order.getSupplierName());
        e.setStatus(order.getStatus().name());
        e.setExpectedDeliveryDate(order.getExpectedDeliveryDate());
        e.setCurrency(order.getCurrency());
        try {
            e.setTotalAmount(order.getTotalAmount().amount());
            e.setSubtotalAmount(order.getSubtotalAmount().amount());
            e.setTaxAmount(order.getTotalTaxAmount().amount());
        } catch (Exception ex) {
            // Lines empty — amounts are zero
        }
        e.setNotes(order.getNotes());
        e.setApprovedBy(order.getApprovedBy());
        return toDomain(jpaRepository.save(e));
    }

    @Override
    public Optional<PurchaseOrder> findById(String id, String tenantId) {
        return jpaRepository.findByIdAndTenantId(id, tenantId).map(this::toDomain);
    }

    @Override
    public Page<PurchaseOrder> findAll(String tenantId, String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            return jpaRepository.findByTenantIdAndStatus(tenantId, status, pageable).map(this::toDomain);
        }
        return jpaRepository.findByTenantId(tenantId, pageable).map(this::toDomain);
    }

    private PurchaseOrder toDomain(PurchaseOrderJpaEntity e) {
        return PurchaseOrder.builder()
            .id(e.getId())
            .tenantId(e.getTenantId())
            .orderNumber(e.getPoNumber())
            .supplierId(e.getSupplierId() != null ? e.getSupplierId() : "")
            .supplierName(e.getSupplierName() != null ? e.getSupplierName() : "")
            .currency(e.getCurrency() != null ? e.getCurrency() : "EUR")
            .orderDate(e.getCreatedAt() != null ? LocalDate.now() : LocalDate.now())
            .expectedDeliveryDate(e.getExpectedDeliveryDate())
            .notes(e.getNotes())
            .lines(List.of())
            .build();
    }
}
