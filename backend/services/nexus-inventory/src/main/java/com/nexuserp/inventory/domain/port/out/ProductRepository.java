package com.nexuserp.inventory.domain.port.out;

import com.nexuserp.inventory.domain.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(UUID id, String tenantId);
    Optional<Product> findByCode(String code, String tenantId);
    Page<Product> findAll(String tenantId, String category, Pageable pageable);
    List<Product> findBelowReorderPoint(String tenantId);
}
