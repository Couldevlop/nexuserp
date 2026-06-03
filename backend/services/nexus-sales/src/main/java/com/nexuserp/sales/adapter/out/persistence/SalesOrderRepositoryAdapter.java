package com.nexuserp.sales.adapter.out.persistence;

import com.nexuserp.sales.domain.model.SalesOrder;
import com.nexuserp.sales.domain.port.out.SalesOrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class SalesOrderRepositoryAdapter implements SalesOrderRepository {

    private final SalesOrderJpaRepository jpaRepository;

    public SalesOrderRepositoryAdapter(SalesOrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public SalesOrder save(SalesOrder order) {
        SalesOrderJpaEntity e = new SalesOrderJpaEntity();
        e.setId(order.getId());
        e.setTenantId(order.getTenantId().value());
        e.setOrderNumber(order.getOrderNumber());
        e.setCustomerId(order.getCustomerId());
        e.setCustomerName(order.getCustomerName());
        e.setCustomerRef(order.getCustomerRef());
        e.setStatus(order.getStatus().name());
        e.setOrderDate(order.getOrderDate());
        e.setRequestedDeliveryDate(order.getRequestedDeliveryDate());
        e.setActualDeliveryDate(order.getActualDeliveryDate());
        e.setCurrency(order.getCurrency());
        if (order.getTotal() != null) {
            e.setTotalAmount(order.getTotal().amount());
        }
        if (order.getSubtotal() != null) {
            e.setSubtotalAmount(order.getSubtotal().amount());
        }
        if (order.getTaxAmount() != null) {
            e.setTaxAmount(order.getTaxAmount().amount());
        }
        e.setShippingAddress(order.getShippingAddress());
        e.setNotes(order.getNotes());
        e.setCreatedBy(order.getCreatedBy());
        return toDomain(jpaRepository.save(e));
    }

    @Override
    public Optional<SalesOrder> findById(UUID id, String tenantId) {
        return jpaRepository.findByIdAndTenantId(id, tenantId).map(this::toDomain);
    }

    @Override
    public Page<SalesOrder> findAll(String tenantId, String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            return jpaRepository.findByTenantIdAndStatus(tenantId, status, pageable).map(this::toDomain);
        }
        return jpaRepository.findByTenantId(tenantId, pageable).map(this::toDomain);
    }

    private SalesOrder toDomain(SalesOrderJpaEntity e) {
        return SalesOrder.builder()
            .id(e.getId())
            .tenantId(e.getTenantId())
            .orderNumber(e.getOrderNumber())
            .customerId(e.getCustomerId())
            .customerName(e.getCustomerName())
            .customerRef(e.getCustomerRef())
            .currency(e.getCurrency() != null ? e.getCurrency() : "EUR")
            .shippingAddress(e.getShippingAddress())
            .notes(e.getNotes())
            .createdBy(e.getCreatedBy())
            .build();
    }
}
