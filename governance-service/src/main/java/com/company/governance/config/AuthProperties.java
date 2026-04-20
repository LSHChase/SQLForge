package com.company.governance.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    private boolean enabled = false;
    private List<String> trustedAuthSources = new ArrayList<String>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getTrustedAuthSources() {
        return trustedAuthSources;
    }

    public void setTrustedAuthSources(List<String> trustedAuthSources) {
        this.trustedAuthSources = trustedAuthSources;
    }
}
