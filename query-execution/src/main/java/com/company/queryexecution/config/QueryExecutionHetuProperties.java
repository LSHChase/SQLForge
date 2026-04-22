package com.company.queryexecution.config;

import com.company.queryexecution.domain.query.QueryExecutionAccessMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "query-execution.hetu")
public class QueryExecutionHetuProperties {

    private boolean enabled = false;
    private final List<QueryExecutionAccessMode> allowedModes = new ArrayList<QueryExecutionAccessMode>();
    private final Jdbc jdbc = new Jdbc();
    private final Rest rest = new Rest();
    private final Client client = new Client();

    public QueryExecutionHetuProperties() {
        allowedModes.add(QueryExecutionAccessMode.JDBC);
        allowedModes.add(QueryExecutionAccessMode.REST);
        allowedModes.add(QueryExecutionAccessMode.CLIENT);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<QueryExecutionAccessMode> getAllowedModes() {
        return allowedModes;
    }

    public Jdbc getJdbc() {
        return jdbc;
    }

    public Rest getRest() {
        return rest;
    }

    public Client getClient() {
        return client;
    }

    public static class Jdbc {

        private String url = "";
        private String username = "";
        private String password = "";
        private int queryTimeoutSeconds = 30;
        private int maxRows = 200;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getQueryTimeoutSeconds() {
            return queryTimeoutSeconds;
        }

        public void setQueryTimeoutSeconds(int queryTimeoutSeconds) {
            this.queryTimeoutSeconds = queryTimeoutSeconds;
        }

        public int getMaxRows() {
            return maxRows;
        }

        public void setMaxRows(int maxRows) {
            this.maxRows = maxRows;
        }
    }

    public static class Rest {

        private String endpoint = "";
        private String authToken = "";
        private int connectTimeoutMs = 3000;
        private int readTimeoutMs = 5000;
        private int maxRows = 200;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAuthToken() {
            return authToken;
        }

        public void setAuthToken(String authToken) {
            this.authToken = authToken;
        }

        public int getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public int getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public void setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }

        public int getMaxRows() {
            return maxRows;
        }

        public void setMaxRows(int maxRows) {
            this.maxRows = maxRows;
        }
    }

    public static class Client {

        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
