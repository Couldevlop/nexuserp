package com.nexuserp.production.domain.service;

import com.nexuserp.core.domain.exception.DomainException;
import com.nexuserp.production.application.command.CreateWorkOrderCommand;
import com.nexuserp.production.domain.model.WorkOrder;
import com.nexuserp.production.domain.port.in.CreateWorkOrderUseCase;
import com.nexuserp.production.domain.port.in.GetWorkOrderUseCase;
import com.nexuserp.production.domain.port.out.WorkOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional
public class ProductionService implements CreateWorkOrderUseCase, GetWorkOrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProductionService.class);
    private final WorkOrderRepository repository;

    public ProductionService(WorkOrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public WorkOrder createWorkOrder(CreateWorkOrderCommand cmd) {
        String woNumber = "WO-" + System.currentTimeMillis();
        WorkOrder workOrder = WorkOrder.builder()
            .id(UUID.randomUUID().toString())
            .tenantId(cmd.tenantId())
            .orderNumber(woNumber)
            .productId(cmd.productId() != null ? cmd.productId() : "")
            .productName(cmd.productName() != null ? cmd.productName() : "")
            .bomId(cmd.bomId())
            .routingId(cmd.routingId())
            .quantityPlanned(cmd.quantityPlanned())
            .plannedStartDate(cmd.plannedStartDate() != null ? cmd.plannedStartDate() : LocalDate.now())
            .plannedEndDate(cmd.plannedEndDate() != null ? cmd.plannedEndDate() : LocalDate.now().plusDays(7))
            .priority(cmd.priority() != null ? cmd.priority() : WorkOrder.Priority.NORMAL)
            .workcenter(cmd.workcenter())
            .notes(cmd.notes())
            .build();

        WorkOrder saved = repository.save(workOrder);
        log.info("WorkOrder created: number={}, tenant={}", saved.getOrderNumber(), cmd.tenantId());
        return saved;
    }

    @Override
    public WorkOrder releaseWorkOrder(UUID id, String tenantId) {
        WorkOrder wo = repository.findById(id.toString(), tenantId)
            .orElseThrow(() -> DomainException.notFound("WorkOrder", id));
        wo.release("system");
        return repository.save(wo);
    }

    @Override
    public WorkOrder startWorkOrder(UUID id, String tenantId, String operatorId) {
        WorkOrder wo = repository.findById(id.toString(), tenantId)
            .orElseThrow(() -> DomainException.notFound("WorkOrder", id));
        wo.start(operatorId);
        return repository.save(wo);
    }

    @Override
    public WorkOrder recordProduction(UUID id, String tenantId, BigDecimal quantity, BigDecimal rejected, String operatorId) {
        WorkOrder wo = repository.findById(id.toString(), tenantId)
            .orElseThrow(() -> DomainException.notFound("WorkOrder", id));
        wo.recordProduction(quantity, rejected, operatorId);
        return repository.save(wo);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkOrder getById(UUID id, String tenantId) {
        return repository.findById(id.toString(), tenantId)
            .orElseThrow(() -> DomainException.notFound("WorkOrder", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WorkOrder> list(String tenantId, String status, Pageable pageable) {
        return repository.findAll(tenantId, status, pageable);
    }
}
