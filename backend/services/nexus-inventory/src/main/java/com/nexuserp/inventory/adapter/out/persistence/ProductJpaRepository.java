package com.nexuserp.inventory.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, UUID> {

    Optional<ProductJpaEntity> findByIdAndTenantId(UUID id, String tenantId);

    Optional<ProductJpaEntity> findByProductCodeAndTenantId(String productCode, String tenantId);

    Page<ProductJpaEntity> findByTenantId(String tenantId, Pageable pageable);

    Page<ProductJpaEntity> findByTenantIdAndCategory(String tenantId, String category, Pageable pageable);

    @Query("SELECT p FROM ProductJpaEntity p WHERE p.tenantId = :tenantId AND p.quantityOnHand <= p.reorderPoint AND p.status = 'ACTIVE'")
    List<ProductJpaEntity> findBelowReorderPoint(String tenantId);
}
