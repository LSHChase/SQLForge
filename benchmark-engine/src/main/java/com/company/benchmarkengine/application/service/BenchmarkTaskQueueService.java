package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.config.BenchmarkTaskExecutionProperties;
import com.company.benchmarkengine.config.BenchmarkTaskQueueProperties;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTask;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskError;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskStatus;
import com.company.benchmarkengine.domain.benchmark.repository.BenchmarkTaskRepository;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.utils.JsonUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BenchmarkTaskQueueService {

    private static final String MODE_DATABASE_WORKER = "database-worker";
    private static final String MODE_LOCAL_PLACEHOLDER = "local-placeholder";
    private static final String MODE_EXTERNAL_FILE_QUEUE = "external-file-queue";

    private final BenchmarkTaskRepository benchmarkTaskRepository;
    private final BenchmarkTaskQueueProperties queueProperties;
    private final BenchmarkTaskExecutionProperties executionProperties;

    public BenchmarkTaskQueueService(BenchmarkTaskRepository benchmarkTaskRepository,
                                     BenchmarkTaskQueueProperties queueProperties,
                                     BenchmarkTaskExecutionProperties executionProperties) {
        this.benchmarkTaskRepository = benchmarkTaskRepository;
        this.queueProperties = queueProperties;
        this.executionProperties = executionProperties;
    }

    public BenchmarkQueueDispatch dispatch(BenchmarkTask task) {
        String mode = effectiveMode();
        if (MODE_EXTERNAL_FILE_QUEUE.equals(mode)) {
            return dispatchToExternalFileQueue(task);
        }
        String evidence = buildEvidence("QUEUE_DISPATCHED", mode, null, null, "DISPATCHED");
        task.appendOperationalNote(evidence);
        benchmarkTaskRepository.saveTask(task);
        return new BenchmarkQueueDispatch(mode, evidence);
    }

    public List<BenchmarkTaskQueueLease> acquireVisibleTasks(Instant visibleBefore) {
        if (visibleBefore == null) {
            return Collections.emptyList();
        }
        String mode = effectiveMode();
        if (MODE_EXTERNAL_FILE_QUEUE.equals(mode)) {
            return acquireExternalFileTasks(visibleBefore);
        }
        return acquireRepositoryTasks(mode, visibleBefore);
    }

    public String effectiveMode() {
        String configured = queueProperties.getMode();
        if (!StringUtils.hasText(configured)) {
            return MODE_DATABASE_WORKER;
        }
        return configured.trim();
    }

    private BenchmarkQueueDispatch dispatchToExternalFileQueue(BenchmarkTask task) {
        try {
            Path queueDir = resolveExternalQueueDir();
            Files.createDirectories(queueDir);
            Path queueFile = queueDir.resolve(queueFileName(task));
            Path tempFile = queueDir.resolve(queueFile.getFileName().toString() + ".tmp");
            Files.write(
                tempFile,
                JsonUtils.toJson(buildEnvelope(task)).getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            );
            move(tempFile, queueFile);
            String evidence = buildEvidence("QUEUE_DISPATCHED", MODE_EXTERNAL_FILE_QUEUE, queueFile, null, "DISPATCHED");
            task.appendOperationalNote(evidence);
            benchmarkTaskRepository.saveTask(task);
            return new BenchmarkQueueDispatch(MODE_EXTERNAL_FILE_QUEUE, evidence);
        } catch (IOException ex) {
            task.appendOperationalNote(buildFailureEvidence("QUEUE_DISPATCH_FAILED", ex));
            task.markFailed(
                new BenchmarkTaskError(
                    ErrorCodeConstants.BENCHMARK_ENGINE_SYSTEM_PIPELINE_NOT_READY,
                    "Benchmark external queue dispatch failed",
                    "Inspect benchmark-engine external queue carrier and replay the queued task after the carrier is healthy.",
                    true
                ),
                Instant.now()
            );
            benchmarkTaskRepository.saveTask(task);
            throw new IllegalStateException("Failed to dispatch benchmark task to external file queue", ex);
        }
    }

    private List<BenchmarkTaskQueueLease> acquireRepositoryTasks(String mode, Instant visibleBefore) {
        List<BenchmarkTask> queuedTasks = benchmarkTaskRepository.findQueuedTasksSubmittedBefore(visibleBefore);
        if (queuedTasks == null || queuedTasks.isEmpty()) {
            return Collections.emptyList();
        }
        int limit = Math.max(queueProperties.getAcquireBatchSize(), 1);
        List<BenchmarkTaskQueueLease> leases = new ArrayList<BenchmarkTaskQueueLease>(Math.min(limit, queuedTasks.size()));
        for (int index = 0; index < queuedTasks.size() && leases.size() < limit; index++) {
            BenchmarkTask task = queuedTasks.get(index);
            leases.add(
                new BenchmarkTaskQueueLease(
                    task,
                    mode,
                    buildEvidence("QUEUE_ACQUIRED", mode, null, null, "ACQUIRED"),
                    new QueueAcknowledgement() {
                        @Override
                        public void acknowledgeSuccess() {
                        }

                        @Override
                        public void acknowledgeFailure(RuntimeException failure) {
                        }
                    }
                )
            );
        }
        return leases;
    }

    private List<BenchmarkTaskQueueLease> acquireExternalFileTasks(Instant visibleBefore) {
        Path queueDir = resolveExternalQueueDir();
        if (!Files.exists(queueDir)) {
            return Collections.emptyList();
        }
        List<Path> queueFiles = listQueueFiles(queueDir, ".queue.json");
        if (queueFiles.isEmpty()) {
            return Collections.emptyList();
        }
        List<BenchmarkTaskQueueLease> leases = new ArrayList<BenchmarkTaskQueueLease>();
        int limit = Math.max(queueProperties.getAcquireBatchSize(), 1);
        for (Path queueFile : queueFiles) {
            if (leases.size() >= limit) {
                break;
            }
            try {
                QueueEnvelope envelope = readEnvelope(queueFile);
                if (envelope == null || envelope.getSubmittedAt() == null || envelope.getSubmittedAt().isAfter(visibleBefore)) {
                    continue;
                }
                Path claimedFile = claimQueueFile(queueFile);
                if (claimedFile == null) {
                    continue;
                }
                BenchmarkTask task = benchmarkTaskRepository.findTaskByTaskId(envelope.getTaskId());
                if (task == null || task.getStatus() != BenchmarkTaskStatus.QUEUED) {
                    cleanupClaimedFile(claimedFile);
                    continue;
                }
                String evidence = buildEvidence("QUEUE_ACQUIRED", MODE_EXTERNAL_FILE_QUEUE, queueFile, claimedFile, "ACQUIRED");
                task.appendOperationalNote(evidence);
                benchmarkTaskRepository.saveTask(task);
                leases.add(
                    new BenchmarkTaskQueueLease(
                        task,
                        MODE_EXTERNAL_FILE_QUEUE,
                        evidence,
                        new ExternalFileQueueAcknowledgement(queueFile, claimedFile, task)
                    )
                );
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to acquire benchmark task from external file queue", ex);
            }
        }
        return leases;
    }

    private Map<String, Object> buildEnvelope(BenchmarkTask task) {
        Map<String, Object> envelope = new LinkedHashMap<String, Object>();
        envelope.put("taskId", task.getTaskId());
        envelope.put("tenantId", task.getTenantId());
        envelope.put("submittedAt", task.getSubmittedAt() == null ? null : task.getSubmittedAt().toString());
        envelope.put("queueMode", MODE_EXTERNAL_FILE_QUEUE);
        envelope.put("queueVisibilityDelayMs", Long.valueOf(executionProperties.getQueueVisibilityDelayMs()));
        envelope.put("readonlyRequired", task.getReadonlyRequired());
        envelope.put(
            "shadowEnvironmentMode",
            task.getShadowEnvironmentMode() == null ? null : task.getShadowEnvironmentMode().name()
        );
        return envelope;
    }

    private String queueFileName(BenchmarkTask task) {
        long submittedAtEpochMs = task.getSubmittedAt() == null ? 0L : task.getSubmittedAt().toEpochMilli();
        return submittedAtEpochMs + "-" + sanitizeFileComponent(task.getTaskId()) + ".queue.json";
    }

    private String sanitizeFileComponent(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return "unknown";
        }
        return rawValue.trim().replaceAll("[^A-Za-z0-9._-]", "-");
    }

    private QueueEnvelope readEnvelope(Path queueFile) throws IOException {
        if (queueFile == null || !Files.exists(queueFile)) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = JsonUtils.objectMapper().readValue(
            Files.readAllBytes(queueFile),
            LinkedHashMap.class
        );
        if (payload == null || payload.get("taskId") == null) {
            return null;
        }
        Instant submittedAt = null;
        if (payload.get("submittedAt") != null) {
            submittedAt = Instant.parse(String.valueOf(payload.get("submittedAt")));
        }
        return new QueueEnvelope(String.valueOf(payload.get("taskId")), submittedAt);
    }

    private Path claimQueueFile(Path queueFile) throws IOException {
        if (queueFile == null || !Files.exists(queueFile)) {
            return null;
        }
        Path claimedFile = queueFile.resolveSibling(queueFile.getFileName().toString() + ".claim");
        move(queueFile, claimedFile);
        return claimedFile;
    }

    private void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private List<Path> listQueueFiles(Path queueDir, String suffix) {
        List<Path> queueFiles = new ArrayList<Path>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(queueDir, "*" + suffix)) {
            for (Path candidate : stream) {
                if (Files.isRegularFile(candidate)) {
                    queueFiles.add(candidate);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to list benchmark external queue files", ex);
        }
        Collections.sort(queueFiles, new Comparator<Path>() {
            @Override
            public int compare(Path left, Path right) {
                return left.getFileName().toString().compareTo(right.getFileName().toString());
            }
        });
        return queueFiles;
    }

    private Path resolveExternalQueueDir() {
        return Paths.get(queueProperties.getExternalFileQueueDir()).toAbsolutePath().normalize();
    }

    private String buildEvidence(String event,
                                 String mode,
                                 Path queueFile,
                                 Path claimedFile,
                                 String status) {
        StringBuilder evidence = new StringBuilder();
        evidence.append(event)
            .append(":mode=").append(mode)
            .append(";queueVisibilityDelayMs=").append(executionProperties.getQueueVisibilityDelayMs())
            .append(";carrierStatus=").append(status);
        if (queueFile != null) {
            evidence.append(";queueMessagePath=").append(queueFile.toAbsolutePath().normalize());
        }
        if (claimedFile != null) {
            evidence.append(";queueClaimPath=").append(claimedFile.toAbsolutePath().normalize());
        }
        return evidence.toString();
    }

    private String buildFailureEvidence(String event, Exception ex) {
        return event
            + ":mode=" + effectiveMode()
            + ";carrierStatus=FAILED"
            + ";reason=" + (ex == null ? "unknown" : sanitizeFileComponent(ex.getClass().getSimpleName()));
    }

    public static QueueEvidence resolveQueueEvidence(BenchmarkTask task) {
        if (task == null || task.getStatusHistory() == null || task.getStatusHistory().isEmpty()) {
            return new QueueEvidence(null, null);
        }
        for (int index = task.getStatusHistory().size() - 1; index >= 0; index--) {
            String note = task.getStatusHistory().get(index).getNote();
            if (!StringUtils.hasText(note) || !note.startsWith("QUEUE_")) {
                continue;
            }
            return new QueueEvidence(resolveValue(note, "mode"), note);
        }
        return new QueueEvidence(null, null);
    }

    private static String resolveValue(String note, String key) {
        if (!StringUtils.hasText(note) || !StringUtils.hasText(key)) {
            return null;
        }
        int colon = note.indexOf(':');
        String payload = colon >= 0 ? note.substring(colon + 1) : note;
        String[] parts = payload.split(";");
        for (String part : parts) {
            int separator = part.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String candidateKey = part.substring(0, separator).trim();
            if (key.equals(candidateKey)) {
                return part.substring(separator + 1).trim();
            }
        }
        return null;
    }

    private final class ExternalFileQueueAcknowledgement implements QueueAcknowledgement {

        private final Path queueFile;
        private final Path claimedFile;
        private final BenchmarkTask task;

        private ExternalFileQueueAcknowledgement(Path queueFile, Path claimedFile, BenchmarkTask task) {
            this.queueFile = queueFile;
            this.claimedFile = claimedFile;
            this.task = task;
        }

        @Override
        public void acknowledgeSuccess() {
            if (queueProperties.isCleanupConsumedFiles()) {
                cleanupClaimedFile(claimedFile);
                return;
            }
            try {
                move(claimedFile, claimedFile.resolveSibling(claimedFile.getFileName().toString() + ".done"));
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to finalize benchmark external queue message", ex);
            }
        }

        @Override
        public void acknowledgeFailure(RuntimeException failure) {
            try {
                task.appendOperationalNote(buildFailureEvidence("QUEUE_RELEASED", failure));
                benchmarkTaskRepository.saveTask(task);
                move(claimedFile, queueFile);
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to release benchmark external queue message", ex);
            }
        }
    }

    private void cleanupClaimedFile(Path claimedFile) {
        try {
            Files.deleteIfExists(claimedFile);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to cleanup consumed benchmark external queue message", ex);
        }
    }

    private static final class QueueEnvelope {

        private final String taskId;
        private final Instant submittedAt;

        private QueueEnvelope(String taskId, Instant submittedAt) {
            this.taskId = taskId;
            this.submittedAt = submittedAt;
        }

        private String getTaskId() {
            return taskId;
        }

        private Instant getSubmittedAt() {
            return submittedAt;
        }
    }

    public static final class BenchmarkQueueDispatch {

        private final String queueMode;
        private final String queueEvidence;

        public BenchmarkQueueDispatch(String queueMode, String queueEvidence) {
            this.queueMode = queueMode;
            this.queueEvidence = queueEvidence;
        }

        public String getQueueMode() {
            return queueMode;
        }

        public String getQueueEvidence() {
            return queueEvidence;
        }
    }

    public static final class BenchmarkTaskQueueLease {

        private final BenchmarkTask task;
        private final String queueMode;
        private final String queueEvidence;
        private final QueueAcknowledgement acknowledgement;

        private BenchmarkTaskQueueLease(BenchmarkTask task,
                                        String queueMode,
                                        String queueEvidence,
                                        QueueAcknowledgement acknowledgement) {
            this.task = task;
            this.queueMode = queueMode;
            this.queueEvidence = queueEvidence;
            this.acknowledgement = acknowledgement;
        }

        public BenchmarkTask getTask() {
            return task;
        }

        public String getQueueMode() {
            return queueMode;
        }

        public String getQueueEvidence() {
            return queueEvidence;
        }

        public void acknowledgeSuccess() {
            acknowledgement.acknowledgeSuccess();
        }

        public void acknowledgeFailure(RuntimeException failure) {
            acknowledgement.acknowledgeFailure(failure);
        }
    }

    public static final class QueueEvidence {

        private final String queueMode;
        private final String queueEvidence;

        public QueueEvidence(String queueMode, String queueEvidence) {
            this.queueMode = queueMode;
            this.queueEvidence = queueEvidence;
        }

        public String getQueueMode() {
            return queueMode;
        }

        public String getQueueEvidence() {
            return queueEvidence;
        }
    }

    private interface QueueAcknowledgement {

        void acknowledgeSuccess();

        void acknowledgeFailure(RuntimeException failure);
    }
}
