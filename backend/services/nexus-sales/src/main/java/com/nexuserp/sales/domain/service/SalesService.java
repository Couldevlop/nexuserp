package com.nexuserp.sales.domain.service;

import com.nexuserp.core.domain.exception.DomainException;
import com.nexuserp.sales.application.command.CreateSalesOrderCommand;
import com.nexuserp.sales.domain.model.SalesOrder;
import com.nexuserp.sales.domain.model.SalesOrderLine;
import com.nexuserp.sales.domain.port.in.CreateSalesOrderUseCase;
import com.nexuserp.sales.domain.port.in.GetSalesOrderUseCase;
import com.nexuserp.sales.domain.port.out.SalesOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Transactional
public class SalesService implements CreateSalesOrderUseCase, GetSalesOrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(SalesService.class);
    private final SalesOrderRepository repository;

    public SalesService(SalesOrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public SalesOrder createSalesOrder(CreateSalesOrderCommand cmd) {
        String orderNumber = "SO-" + System.currentTimeMillis();
        AtomicInteger lineNum = new AtomicInteger(1);

        SalesOrder.Builder builder = SalesOrder.builder()
            .tenantId(cmd.tenantId())
            .orderNumber(orderNumber)
            .customerId(cmd.customerId())
            .customerName(cmd.customerName())
            .customerRef(cmd.customerRef())
            .requestedDeliveryDate(cmd.requestedDeliveryDate())
            .currency(cmd.currency() != null ? cmd.currency() : "EUR")
            .shippingAddress(cmd.shippingAddress())
            .notes(cmd.notes())
            .createdBy(cmd.createdBy());

        if (cmd.lines() != null) {
            cmd.lines().forEach(l -> builder.addLine(SalesOrderLine.of(
                lineNum.getAndIncrement(), l.productCode(), l.productName(),
                l.quantity(), l.unitPrice(), l.taxRate())));
        }

        SalesOrder saved = repository.save(builder.build());
        log.info("SO created: number={}, tenant={}", saved.getOrderNumber(), cmd.tenantId());
        return saved;
    }

    @Override
    public SalesOrder confirmSalesOrder(UUID id, String tenantId, String confirmedBy) {
        SalesOrder order = repository.findById(id, tenantId)
            .orElseThrow(() -> DomainException.notFound("SalesOrder", id));
        order.confirm();
        return repository.save(order);
    }

    @Override
    public SalesOrder cancelSalesOrder(UUID id, String tenantId, String reason) {
        SalesOrder order = repository.findById(id, tenantId)
            .orElseThrow(() -> DomainException.notFound("SalesOrder", id));
        order.cancel(reason);
        return repository.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public SalesOrder getById(UUID id, String tenantId) {
        return repository.findById(id, tenantId)
            .orElseThrow(() -> DomainException.notFound("SalesOrder", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SalesOrder> list(String tenantId, String status, Pageable pageable) {
        return repository.findAll(tenantId, status, pageable);
    }
}
