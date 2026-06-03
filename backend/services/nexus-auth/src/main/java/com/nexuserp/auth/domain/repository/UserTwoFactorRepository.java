package com.nexuserp.auth.domain.repository;

import com.nexuserp.auth.domain.model.UserTwoFactor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserTwoFactorRepository extends JpaRepository<UserTwoFactor, UUID> {
    Optional<UserTwoFactor> findByUserIdAndTenantId(UUID userId, String tenantId);
    boolean existsByUserIdAndTenantIdAndStatus(UUID userId, String tenantId, UserTwoFactor.TwoFactorStatus status);
}
