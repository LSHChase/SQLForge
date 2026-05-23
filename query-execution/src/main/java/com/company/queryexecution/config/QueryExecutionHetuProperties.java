package com.company.queryexecution.config;

import com.company.queryexecution.domain.query.QueryExecutionAccessMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "query-execution.hetu")
public class QueryExecutionHetuProperties {

    private boolean enabled = false;
    private final List<QueryExecutionAccessMode> allowedModes = new ArrayList<QueryExecutionAccessMode>();
    private final Calibration calibration = new Calibration();
    private final ClusterEvidence clusterEvidence = new ClusterEvidence();
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

    public Calibration getCalibration() {
        return calibration;
    }

    public ClusterEvidence getClusterEvidence() {
        return clusterEvidence;
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

    public static class Calibration {

        private String profile = "REPO_CLOSED_BASELINE";
        private final List<QueryExecutionAccessMode> routeOrder = new ArrayList<QueryExecutionAccessMode>();
        private boolean skipUnreadyModes = true;

        public Calibration() {
            routeOrder.add(QueryExecutionAccessMode.JDBC);
            routeOrder.add(QueryExecutionAccessMode.REST);
            routeOrder.add(QueryExecutionAccessMode.CLIENT);
        }

        public String getProfile() {
            return profile;
        }

        public void setProfile(String profile) {
            this.profile = profile;
        }

        public List<QueryExecutionAccessMode> getRouteOrder() {
            return routeOrder;
        }

        public boolean isSkipUnreadyModes() {
            return skipUnreadyModes;
        }

        public void setSkipUnreadyModes(boolean skipUnreadyModes) {
            this.skipUnreadyModes = skipUnreadyModes;
        }
    }

    public static class ClusterEvidence {

        private String evidenceSource = "REPO_CLOSED_CONFIGURATION";
        private String environmentLabel = "repo-default";
        private String clusterName = "UNSPECIFIED";
        private String coordinatorEndpoint = "";
        private String runbookRef = "docs/deployments/hetu-test-environment-deployment-runbook.md";
        private String evidenceRef = "HARN-016/INBOX-002";
        private String readonlyBoundary = "REPO_CLOSED_DEFAULT";
        private String liveVerificationStatus = "PENDING_ENV_WINDOW";
        private String evidenceNotes = "";

        public String getEvidenceSource() {
            return evidenceSource;
        }

        public void setEvidenceSource(String evidenceSource) {
            this.evidenceSource = evidenceSource;
        }

        public String getEnvironmentLabel() {
            return environmentLabel;
        }

        public void setEnvironmentLabel(String environmentLabel) {
            this.environmentLabel = environmentLabel;
        }

        public String getClusterName() {
            return clusterName;
        }

        public void setClusterName(String clusterName) {
            this.clusterName = clusterName;
        }

        public String getCoordinatorEndpoint() {
            return coordinatorEndpoint;
        }

        public void setCoordinatorEndpoint(String coordinatorEndpoint) {
            this.coordinatorEndpoint = coordinatorEndpoint;
        }

        public String getRunbookRef() {
            return runbookRef;
        }

        public void setRunbookRef(String runbookRef) {
            this.runbookRef = runbookRef;
        }

        public String getEvidenceRef() {
            return evidenceRef;
        }

        public void setEvidenceRef(String evidenceRef) {
            this.evidenceRef = evidenceRef;
        }

        public String getReadonlyBoundary() {
            return readonlyBoundary;
        }

        public void setReadonlyBoundary(String readonlyBoundary) {
            this.readonlyBoundary = readonlyBoundary;
        }

        public String getLiveVerificationStatus() {
            return liveVerificationStatus;
        }

        public void setLiveVerificationStatus(String liveVerificationStatus) {
            this.liveVerificationStatus = liveVerificationStatus;
        }

        public String getEvidenceNotes() {
            return evidenceNotes;
        }

        public void setEvidenceNotes(String evidenceNotes) {
            this.evidenceNotes = evidenceNotes;
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
        private String endpoint = "";
        private String user = "";
        private String source = "sqlforge-query-execution";
        private String catalog = "";
        private String schema = "";
        private String authToken = "";
        private int connectTimeoutMs = 3000;
        private int readTimeoutMs = 5000;
        private int maxRows = 200;
        private int maxPages = 10;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getCatalog() {
            return catalog;
        }

        public void setCatalog(String catalog) {
            this.catalog = catalog;
        }

        public String getSchema() {
            return schema;
        }

        public void setSchema(String schema) {
            this.schema = schema;
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

        public int getMaxPages() {
            return maxPages;
        }

        public void setMaxPages(int maxPages) {
            this.maxPages = maxPages;
        }
    }
}
