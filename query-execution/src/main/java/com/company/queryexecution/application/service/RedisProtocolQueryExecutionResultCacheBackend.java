package com.company.queryexecution.application.service;

import com.company.queryexecution.config.QueryExecutionCacheBackendProperties;
import com.company.queryexecution.domain.query.QueryExecutionStep;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.utils.JsonUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

final class RedisProtocolQueryExecutionResultCacheBackend implements QueryExecutionResultCacheBackend {

    private final QueryExecutionCacheBackendProperties properties;
    private final BackendDescriptor descriptor;

    RedisProtocolQueryExecutionResultCacheBackend(QueryExecutionCacheBackendProperties properties) {
        this.properties = properties;
        this.descriptor = new BackendDescriptor(
            "REDIS",
            trimToDefault(properties.getProviderName(), "REDIS"),
            "PROVIDER_NATIVE_RESP",
            trimToDefault(properties.getEnvironmentLabel(), "environment-backed"),
            true
        );
    }

    @Override
    public BackendDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public CacheEntryReadResult read(String cacheKey) {
        try {
            Object value = execute("GET", cacheKey);
            if (value == null) {
                return CacheEntryReadResult.miss("providerReadStatus=MISS;providerCommand=GET");
            }
            QueryExecutionStep step = QueryExecutionStepSnapshot.fromJson(String.valueOf(value)).toStep();
            return CacheEntryReadResult.hit(step, "providerReadStatus=HIT;providerCommand=GET");
        } catch (Exception ex) {
            return CacheEntryReadResult.unavailable(
                "providerReadStatus=UNAVAILABLE;providerCommand=GET",
                sanitize(ex.getMessage())
            );
        }
    }

    @Override
    public CacheEntryWriteResult write(String cacheKey, QueryExecutionStep step) {
        try {
            Object result = execute("SET", cacheKey, QueryExecutionStepSnapshot.fromStep(step).toJson());
            if ("OK".equalsIgnoreCase(String.valueOf(result))) {
                return CacheEntryWriteResult.written("providerWriteStatus=STORED;providerCommand=SET");
            }
            return CacheEntryWriteResult.failed(
                "providerWriteStatus=REJECTED;providerCommand=SET",
                "Unexpected Redis SET response"
            );
        } catch (Exception ex) {
            return CacheEntryWriteResult.failed(
                "providerWriteStatus=UNAVAILABLE;providerCommand=SET",
                sanitize(ex.getMessage())
            );
        }
    }

    @Override
    public CacheEntryInvalidateResult invalidateByPrefix(String cacheKeyPrefix) {
        try {
            List<String> keys = keys(cacheKeyPrefix + "*");
            int invalidated = 0;
            for (String key : keys) {
                Object deleted = execute("DEL", key);
                if (deleted instanceof Number && ((Number) deleted).intValue() > 0) {
                    invalidated++;
                }
            }
            return CacheEntryInvalidateResult.completed(
                invalidated,
                "providerInvalidateStatus=COMPLETED;providerCommand=KEYS+DEL"
            );
        } catch (Exception ex) {
            return CacheEntryInvalidateResult.failed(
                "providerInvalidateStatus=UNAVAILABLE;providerCommand=KEYS+DEL",
                sanitize(ex.getMessage())
            );
        }
    }

    @Override
    public CacheEntryCountResult countByPrefix(String cacheKeyPrefix) {
        try {
            List<String> keys = keys(cacheKeyPrefix + "*");
            return CacheEntryCountResult.completed(keys.size(), "providerCountStatus=COMPLETED;providerCommand=KEYS");
        } catch (Exception ex) {
            return CacheEntryCountResult.failed(
                "providerCountStatus=UNAVAILABLE;providerCommand=KEYS",
                sanitize(ex.getMessage())
            );
        }
    }

    @Override
    public CacheBackendVerifyResult verify() {
        try {
            Object result = execute("PING");
            if ("PONG".equalsIgnoreCase(String.valueOf(result))) {
                return CacheBackendVerifyResult.available("providerVerifyStatus=AVAILABLE;providerCommand=PING");
            }
            return CacheBackendVerifyResult.unavailable(
                "providerVerifyStatus=UNEXPECTED;providerCommand=PING",
                "Unexpected Redis PING response"
            );
        } catch (Exception ex) {
            return CacheBackendVerifyResult.unavailable(
                "providerVerifyStatus=UNAVAILABLE;providerCommand=PING",
                sanitize(ex.getMessage())
            );
        }
    }

    private List<String> keys(String pattern) throws IOException {
        Object value = execute("KEYS", pattern);
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }
        List<?> raw = (List<?>) value;
        List<String> keys = new ArrayList<String>();
        for (Object item : raw) {
            if (item != null) {
                keys.add(String.valueOf(item));
            }
        }
        return keys;
    }

    private Object execute(String... args) throws IOException {
        QueryExecutionCacheBackendProperties.Redis redis = properties.getRedis();
        if (redis == null || !StringUtils.hasText(redis.getHost())) {
            throw new IOException("Redis backend host is not configured");
        }
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(redis.getHost().trim(), redis.getPort()), redis.getConnectTimeoutMs());
        socket.setSoTimeout(redis.getReadTimeoutMs());
        try {
            BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream());
            BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
            authenticateIfNeeded(redis, output, input);
            selectDatabaseIfNeeded(redis, output, input);
            writeCommand(output, args);
            output.flush();
            return readResp(input);
        } finally {
            socket.close();
        }
    }

    private void authenticateIfNeeded(QueryExecutionCacheBackendProperties.Redis redis,
                                      BufferedOutputStream output,
                                      BufferedInputStream input) throws IOException {
        if (!StringUtils.hasText(redis.getPassword())) {
            return;
        }
        writeCommand(output, "AUTH", redis.getPassword().trim());
        output.flush();
        readResp(input);
    }

    private void selectDatabaseIfNeeded(QueryExecutionCacheBackendProperties.Redis redis,
                                        BufferedOutputStream output,
                                        BufferedInputStream input) throws IOException {
        if (redis.getDatabase() <= 0) {
            return;
        }
        writeCommand(output, "SELECT", String.valueOf(redis.getDatabase()));
        output.flush();
        readResp(input);
    }

    private void writeCommand(BufferedOutputStream output, String... args) throws IOException {
        output.write(("*" + args.length + "\r\n").getBytes(StandardCharsets.UTF_8));
        for (String arg : args) {
            byte[] bytes = (arg == null ? "" : arg).getBytes(StandardCharsets.UTF_8);
            output.write(("$" + bytes.length + "\r\n").getBytes(StandardCharsets.UTF_8));
            output.write(bytes);
            output.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
    }

    private Object readResp(BufferedInputStream input) throws IOException {
        int type = input.read();
        if (type == -1) {
            throw new IOException("Redis closed connection");
        }
        if (type == '+') {
            return readLine(input);
        }
        if (type == '-') {
            throw new IOException(readLine(input));
        }
        if (type == ':') {
            return Long.valueOf(readLine(input));
        }
        if (type == '$') {
            int length = Integer.parseInt(readLine(input));
            if (length < 0) {
                return null;
            }
            byte[] bytes = readBytes(input, length);
            readBytes(input, 2);
            return new String(bytes, StandardCharsets.UTF_8);
        }
        if (type == '*') {
            int count = Integer.parseInt(readLine(input));
            if (count < 0) {
                return Collections.emptyList();
            }
            List<Object> values = new ArrayList<Object>();
            for (int index = 0; index < count; index++) {
                values.add(readResp(input));
            }
            return values;
        }
        throw new IOException("Unsupported Redis response type");
    }

    private String readLine(BufferedInputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int previous = -1;
        int current;
        while ((current = input.read()) != -1) {
            if (previous == '\r' && current == '\n') {
                byte[] raw = buffer.toByteArray();
                return new String(raw, 0, raw.length - 1, StandardCharsets.UTF_8);
            }
            buffer.write(current);
            previous = current;
        }
        throw new IOException("Redis response line is incomplete");
    }

    private byte[] readBytes(BufferedInputStream input, int length) throws IOException {
        byte[] bytes = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = input.read(bytes, offset, length - offset);
            if (read == -1) {
                throw new IOException("Redis bulk response is incomplete");
            }
            offset += read;
        }
        return bytes;
    }

    private static String trimToDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private static String sanitize(String value) {
        if (!StringUtils.hasText(value)) {
            return "provider backend unavailable";
        }
        return value.replace(';', ',').replace('\n', ' ').replace('\r', ' ');
    }

    public static final class QueryExecutionStepSnapshot {

        private DataSourceTypeEnum targetEngine;
        private List<Map<String, Object>> rows;
        private long elapsedMs;
        private long scannedRows;
        private boolean cacheHit;
        private boolean accelerationApplied;
        private String executionMode;
        private List<String> attemptedModes;
        private String routeProfile;
        private List<String> routeOrder;
        private String routeEvidenceSource;
        private String routeVerificationStatus;
        private String cacheGovernanceStatus;
        private String cacheGovernanceEvidence;

        public static QueryExecutionStepSnapshot fromStep(QueryExecutionStep step) {
            QueryExecutionStepSnapshot snapshot = new QueryExecutionStepSnapshot();
            snapshot.targetEngine = step.getTargetEngine();
            snapshot.rows = step.getRows();
            snapshot.elapsedMs = step.getElapsedMs();
            snapshot.scannedRows = step.getScannedRows();
            snapshot.cacheHit = step.isCacheHit();
            snapshot.accelerationApplied = step.isAccelerationApplied();
            snapshot.executionMode = step.getExecutionMode();
            snapshot.attemptedModes = step.getAttemptedModes();
            snapshot.routeProfile = step.getRouteProfile();
            snapshot.routeOrder = step.getRouteOrder();
            snapshot.routeEvidenceSource = step.getRouteEvidenceSource();
            snapshot.routeVerificationStatus = step.getRouteVerificationStatus();
            snapshot.cacheGovernanceStatus = step.getCacheGovernanceStatus();
            snapshot.cacheGovernanceEvidence = step.getCacheGovernanceEvidence();
            return snapshot;
        }

        public static QueryExecutionStepSnapshot fromJson(String json) {
            return JsonUtils.fromJson(json, QueryExecutionStepSnapshot.class);
        }

        public QueryExecutionStep toStep() {
            return new QueryExecutionStep(
                targetEngine,
                rows == null ? Collections.<Map<String, Object>>emptyList() : rows,
                elapsedMs,
                scannedRows,
                cacheHit,
                accelerationApplied,
                executionMode,
                attemptedModes == null ? Collections.<String>emptyList() : attemptedModes,
                routeProfile,
                routeOrder == null ? Collections.<String>emptyList() : routeOrder,
                routeEvidenceSource,
                routeVerificationStatus,
                cacheGovernanceStatus,
                cacheGovernanceEvidence
            );
        }

        public String toJson() {
            return JsonUtils.toJson(this);
        }

        public DataSourceTypeEnum getTargetEngine() {
            return targetEngine;
        }

        public void setTargetEngine(DataSourceTypeEnum targetEngine) {
            this.targetEngine = targetEngine;
        }

        public List<Map<String, Object>> getRows() {
            return rows;
        }

        public void setRows(List<Map<String, Object>> rows) {
            this.rows = rows;
        }

        public long getElapsedMs() {
            return elapsedMs;
        }

        public void setElapsedMs(long elapsedMs) {
            this.elapsedMs = elapsedMs;
        }

        public long getScannedRows() {
            return scannedRows;
        }

        public void setScannedRows(long scannedRows) {
            this.scannedRows = scannedRows;
        }

        public boolean isCacheHit() {
            return cacheHit;
        }

        public void setCacheHit(boolean cacheHit) {
            this.cacheHit = cacheHit;
        }

        public boolean isAccelerationApplied() {
            return accelerationApplied;
        }

        public void setAccelerationApplied(boolean accelerationApplied) {
            this.accelerationApplied = accelerationApplied;
        }

        public String getExecutionMode() {
            return executionMode;
        }

        public void setExecutionMode(String executionMode) {
            this.executionMode = executionMode;
        }

        public List<String> getAttemptedModes() {
            return attemptedModes;
        }

        public void setAttemptedModes(List<String> attemptedModes) {
            this.attemptedModes = attemptedModes;
        }

        public String getRouteProfile() {
            return routeProfile;
        }

        public void setRouteProfile(String routeProfile) {
            this.routeProfile = routeProfile;
        }

        public List<String> getRouteOrder() {
            return routeOrder;
        }

        public void setRouteOrder(List<String> routeOrder) {
            this.routeOrder = routeOrder;
        }

        public String getRouteEvidenceSource() {
            return routeEvidenceSource;
        }

        public void setRouteEvidenceSource(String routeEvidenceSource) {
            this.routeEvidenceSource = routeEvidenceSource;
        }

        public String getRouteVerificationStatus() {
            return routeVerificationStatus;
        }

        public void setRouteVerificationStatus(String routeVerificationStatus) {
            this.routeVerificationStatus = routeVerificationStatus;
        }

        public String getCacheGovernanceStatus() {
            return cacheGovernanceStatus;
        }

        public void setCacheGovernanceStatus(String cacheGovernanceStatus) {
            this.cacheGovernanceStatus = cacheGovernanceStatus;
        }

        public String getCacheGovernanceEvidence() {
            return cacheGovernanceEvidence;
        }

        public void setCacheGovernanceEvidence(String cacheGovernanceEvidence) {
            this.cacheGovernanceEvidence = cacheGovernanceEvidence;
        }
    }
}
