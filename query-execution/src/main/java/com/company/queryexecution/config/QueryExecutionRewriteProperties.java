package com.company.queryexecution.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "query-execution.rewrite")
public class QueryExecutionRewriteProperties {

    private boolean developmentDirectSuccessEnabled = false;

    public boolean isDevelopmentDirectSuccessEnabled() {
        return developmentDirectSuccessEnabled;
    }

    public void setDevelopmentDirectSuccessEnabled(boolean developmentDirectSuccessEnabled) {
        this.developmentDirectSuccessEnabled = developmentDirectSuccessEnabled;
    }
}
