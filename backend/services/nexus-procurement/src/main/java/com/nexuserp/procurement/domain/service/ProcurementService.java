package com.nexuserp.procurement.domain.service;

import com.nexuserp.core.domain.exception.DomainException;
import com.nexuserp.procurement.application.command.CreatePurchaseOrderCommand;
import com.nexuserp.procurement.domain.model.PurchaseOrder;
import com.nexuserp.procurement.domain.model.PurchaseOrderLine;
import com.nexuserp.procurement.domain.port.in.CreatePurchaseOrderUseCase;
import com.nexuserp.procurement.domain.port.in.GetPurchaseOrderUseCase;
import com.nexuserp.procurement.domain.port.out.PurchaseOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProcurementService implements CreatePurchaseOrderUseCase, GetPurchaseOrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcurementService.class);
    private final PurchaseOrderRepository repository;

    public ProcurementService(PurchaseOrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public PurchaseOrder createPurchaseOrder(CreatePurchaseOrderCommand cmd) {
        String poNumber = "PO-" + System.currentTimeMillis();
        AtomicInteger lineNum = new AtomicInteger(1);
        List<PurchaseOrderLine> lines = cmd.lines() != null
            ? cmd.lines().stream().map(l -> PurchaseOrderLine.of(
                lineNum.getAndIncrement(), l.productCode(), l.description(),
                l.quantity(), l.unitPrice(), l.taxRate())).collect(Collectors.toList())
            : List.of();

        PurchaseOrder order = PurchaseOrder.builder()
            .id(UUID.randomUUID().toString())
            .tenantId(cmd.tenantId())
            .orderNumber(poNumber)
            .supplierId(cmd.supplierId() != null ? cmd.supplierId().toString() : "")
            .supplierName(cmd.supplierName())
            .currency(cmd.currency() != null ? cmd.currency() : "EUR")
            .orderDate(LocalDate.now())
            .expectedDeliveryDate(cmd.expectedDeliveryDate())
            .notes(cmd.notes())
            .lines(lines)
            .build();

        PurchaseOrder saved = repository.save(order);
        log.info("PO created: number={}, tenant={}", saved.getOrderNumber(), cmd.tenantId());
        return saved;
    }

    @Override
    public PurchaseOrder approvePurchaseOrder(UUID id, String tenantId, String approvedBy) {
        PurchaseOrder order = repository.findById(id.toString(), tenantId)
            .orElseThrow(() -> DomainException.notFound("PurchaseOrder", id));
        order.submit(approvedBy);
        order.approve(approvedBy);
        return repository.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrder getById(UUID id, String tenantId) {
        return repository.findById(id.toString(), tenantId)
            .orElseThrow(() -> DomainException.notFound("PurchaseOrder", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseOrder> list(String tenantId, String status, Pageable pageable) {
        return repository.findAll(tenantId, status, pageable);
    }
}
