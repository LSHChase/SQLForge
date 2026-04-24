package com.company.benchmarkengine.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.benchmarkengine.config.BenchmarkArtifactStorageProperties;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifact;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifactKind;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportFormat;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BenchmarkArtifactStorageServiceTest {

    @Test
    void shouldCleanupStaleFilesWhenExternalizingLatestArtifactSet(@TempDir Path tempDir) throws Exception {
        BenchmarkArtifactStorageProperties properties = new BenchmarkArtifactStorageProperties();
        properties.setBaseDir(tempDir.resolve("artifacts").toString());
        BenchmarkArtifactStorageService service = new BenchmarkArtifactStorageService(properties);

        BenchmarkReportArtifact pdfArtifact = artifact("pdf-export", "benchmark-report-report-001.pdf", BenchmarkReportFormat.PDF, "pdf-v1");
        BenchmarkReportArtifact htmlArtifact = artifact("html-export", "benchmark-report-report-001.html", BenchmarkReportFormat.HTML, "html-v1");
        service.externalize("report-001", Arrays.asList(pdfArtifact, htmlArtifact));

        BenchmarkReportArtifact updatedPdfArtifact = artifact("pdf-export", "benchmark-report-report-001.pdf", BenchmarkReportFormat.PDF, "pdf-v2");
        service.externalize("report-001", Collections.singletonList(updatedPdfArtifact));

        Path pdfPath = tempDir.resolve("artifacts/report-001/benchmark-report-report-001.pdf");
        Path htmlPath = tempDir.resolve("artifacts/report-001/benchmark-report-report-001.html");
        assertTrue(Files.exists(pdfPath));
        assertEquals("pdf-v2", new String(Files.readAllBytes(pdfPath)));
        assertTrue(Files.notExists(htmlPath));
    }

    @Test
    void shouldRecoverMissingFileFromSuppliedArtifact(@TempDir Path tempDir) throws Exception {
        BenchmarkArtifactStorageProperties properties = new BenchmarkArtifactStorageProperties();
        properties.setBaseDir(tempDir.resolve("artifacts").toString());
        BenchmarkArtifactStorageService service = new BenchmarkArtifactStorageService(properties);

        BenchmarkReportArtifact persistedArtifact = service.externalize(
            "report-001",
            artifact("pdf-export", "benchmark-report-report-001.pdf", BenchmarkReportFormat.PDF, "pdf-v1")
        ).withExportId("export-benchmark-report-001-pdf-export");
        Files.delete(Paths.get(URI.create(persistedArtifact.getStorageUri())));

        BenchmarkArtifactLoadResult loadResult = service.loadOrRecover(
            "report-001",
            persistedArtifact,
            () -> artifact("pdf-export", "benchmark-report-report-001.pdf", BenchmarkReportFormat.PDF, "pdf-v2")
        );

        assertTrue(loadResult.isRecovered());
        assertEquals("RECOVERED_FROM_REPORT_SNAPSHOT", loadResult.getRecoveryStatus());
        assertEquals("export-benchmark-report-001-pdf-export", loadResult.getResolvedArtifact().getExportId());
        assertEquals("pdf-v2", new String(loadResult.getRenderedReport().getContent()));
    }

    private BenchmarkReportArtifact artifact(String artifactKey,
                                             String fileName,
                                             BenchmarkReportFormat format,
                                             String content) {
        return new BenchmarkReportArtifact(
            artifactKey,
            BenchmarkReportArtifactKind.REPORT_EXPORT,
            format,
            fileName,
            format.getContentType(),
            Integer.valueOf(content.length()),
            "checksum",
            null,
            null,
            null,
            content
        );
    }
}
