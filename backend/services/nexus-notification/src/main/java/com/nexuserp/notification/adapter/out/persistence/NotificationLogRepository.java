package com.nexuserp.notification.adapter.out.persistence;

import com.nexuserp.notification.domain.model.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, String> {
    List<NotificationLog> findByTenantIdOrderBySentAtDesc(String tenantId);
    long countByTenantIdAndStatus(String tenantId, NotificationLog.Status status);
}
