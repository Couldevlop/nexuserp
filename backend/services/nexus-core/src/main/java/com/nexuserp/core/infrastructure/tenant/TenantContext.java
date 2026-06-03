package com.nexuserp.core.infrastructure.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contexte multi-tenant basé sur ThreadLocal.
 * Propagation assurée par TenantInterceptor (REST) et Kafka header propagation.
 */
public final class TenantContext {

    private static final Logger log = LoggerFactory.getLogger(TenantContext.class);
    private static final ThreadLocal<String> CURRENT_TENANT = new InheritableThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_USER = new InheritableThreadLocal<>();

    private TenantContext() {}

    public static void setTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("TenantId cannot be null or blank");
        }
        CURRENT_TENANT.set(tenantId);
        log.debug("Tenant context set: {}", tenantId);
    }

    public static String getTenantId() {
        String tenantId = CURRENT_TENANT.get();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant ID in current context — ensure TenantInterceptor is active");
        }
        return tenantId;
    }

    public static String getTenantIdOrNull() {
        return CURRENT_TENANT.get();
    }

    public static void setUserId(String userId) {
        CURRENT_USER.set(userId);
    }

    public static String getUserId() {
        return CURRENT_USER.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
        CURRENT_USER.remove();
        log.debug("Tenant context cleared");
    }

    public static boolean hasTenant() {
        return CURRENT_TENANT.get() != null;
    }
}
