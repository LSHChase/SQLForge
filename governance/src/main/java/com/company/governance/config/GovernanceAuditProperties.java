package com.company.governance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "governance.audit")
public class GovernanceAuditProperties {

    private boolean smokeForcePrimaryDeliveryFailureEnabled = false;
    private String smokeForceTracePrefix = "SMOKE-FORCE-AUDIT-FALLBACK";

    public boolean isSmokeForcePrimaryDeliveryFailureEnabled() {
        return smokeForcePrimaryDeliveryFailureEnabled;
    }

    public void setSmokeForcePrimaryDeliveryFailureEnabled(boolean smokeForcePrimaryDeliveryFailureEnabled) {
        this.smokeForcePrimaryDeliveryFailureEnabled = smokeForcePrimaryDeliveryFailureEnabled;
    }

    public String getSmokeForceTracePrefix() {
        return smokeForceTracePrefix;
    }

    public void setSmokeForceTracePrefix(String smokeForceTracePrefix) {
        this.smokeForceTracePrefix = smokeForceTracePrefix;
    }
}
