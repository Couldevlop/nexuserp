package com.nexuserp.production.adapter.out.persistence;

import com.nexuserp.production.domain.model.WorkOrder;
import com.nexuserp.production.domain.port.out.WorkOrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Component
public class WorkOrderRepositoryAdapter implements WorkOrderRepository {

    private final WorkOrderJpaRepository jpaRepository;

    public WorkOrderRepositoryAdapter(WorkOrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public WorkOrder save(WorkOrder wo) {
        WorkOrderJpaEntity e = new WorkOrderJpaEntity();
        e.setId(wo.getId());
        e.setTenantId(wo.getTenantId());
        e.setOrderNumber(wo.getOrderNumber());
        e.setProductId(wo.getProductId());
        e.setProductName(wo.getProductName());
        e.setBomId(wo.getBomId());
        e.setRoutingId(wo.getRoutingId());
        e.setStatus(wo.getStatus().name());
        e.setPriority(wo.getPriority().name());
        e.setQuantityPlanned(wo.getQuantityPlanned());
        e.setQuantityProduced(wo.getQuantityProduced());
        e.setQuantityRejected(wo.getQuantityRejected());
        e.setPlannedStartDate(wo.getPlannedStartDate());
        e.setPlannedEndDate(wo.getPlannedEndDate());
        e.setActualStartDate(wo.getActualStartDate());
        e.setActualEndDate(wo.getActualEndDate());
        e.setWorkcenter(wo.getWorkcenter());
        e.setOperator(wo.getOperator());
        e.setNotes(wo.getNotes());
        return toDomain(jpaRepository.save(e));
    }

    @Override
    public Optional<WorkOrder> findById(String id, String tenantId) {
        return jpaRepository.findByIdAndTenantId(id, tenantId).map(this::toDomain);
    }

    @Override
    public Page<WorkOrder> findAll(String tenantId, String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            return jpaRepository.findByTenantIdAndStatus(tenantId, status, pageable).map(this::toDomain);
        }
        return jpaRepository.findByTenantId(tenantId, pageable).map(this::toDomain);
    }

    private WorkOrder toDomain(WorkOrderJpaEntity e) {
        return WorkOrder.builder()
            .id(e.getId())
            .tenantId(e.getTenantId())
            .orderNumber(e.getOrderNumber())
            .productId(e.getProductId() != null ? e.getProductId() : "")
            .productName(e.getProductName() != null ? e.getProductName() : "")
            .bomId(e.getBomId())
            .routingId(e.getRoutingId())
            .quantityPlanned(e.getQuantityPlanned() != null ? e.getQuantityPlanned() : BigDecimal.ONE)
            .plannedStartDate(e.getPlannedStartDate() != null ? e.getPlannedStartDate() : LocalDate.now())
            .plannedEndDate(e.getPlannedEndDate() != null ? e.getPlannedEndDate() : LocalDate.now().plusDays(7))
            .priority(e.getPriority() != null ? WorkOrder.Priority.valueOf(e.getPriority()) : WorkOrder.Priority.NORMAL)
            .workcenter(e.getWorkcenter())
            .notes(e.getNotes())
            .build();
    }
}
