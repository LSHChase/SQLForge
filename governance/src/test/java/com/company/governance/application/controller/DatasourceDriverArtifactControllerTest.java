package com.company.governance.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.governance.application.controller.vo.JdbcDriverArtifactVO;
import com.company.governance.application.service.JdbcDriverArtifactApplicationService;
import com.company.governance.config.GovernanceDatasourceDriverProperties;
import com.company.sqlforge.common.exception.GlobalExceptionHandler;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class DatasourceDriverArtifactControllerTest {

    @Test
    void shouldExposeJdbcDriverArtifactUploadAndQueryEndpoints() throws Exception {
        JdbcDriverArtifactApplicationService service = org.mockito.Mockito.mock(JdbcDriverArtifactApplicationService.class);
        JdbcDriverArtifactVO artifact = sampleArtifact();
        when(service.upload(eq("tenant-a"), eq("HETU"), eq("io.prestosql.jdbc.PrestoDriver"), eq("351"), any()))
            .thenReturn(artifact);
        when(service.list("tenant-a")).thenReturn(Collections.singletonList(artifact));
        when(service.find("tenant-a", "artifact-001")).thenReturn(artifact);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DatasourceDriverArtifactController(service))
            .setControllerAdvice(
                new DatasourceDriverUploadExceptionHandler(new GovernanceDatasourceDriverProperties()),
                new GlobalExceptionHandler()
            )
            .build();

        mockMvc.perform(multipart("/api/governance/datasource-drivers")
                .file(new MockMultipartFile("file", "hetu-driver.jar", "application/java-archive", new byte[]{1, 2, 3}))
                .param("tenantId", "tenant-a")
                .param("engineType", "HETU")
                .param("driverClassName", "io.prestosql.jdbc.PrestoDriver")
                .param("versionLabel", "351"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.artifactId").value("artifact-001"))
            .andExpect(jsonPath("$.engineType").value("HETU"));

        mockMvc.perform(get("/api/governance/datasource-drivers").param("tenantId", "tenant-a"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].artifactId").value("artifact-001"));

        mockMvc.perform(get("/api/governance/datasource-drivers/artifact-001").param("tenantId", "tenant-a"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.driverClassName").value("io.prestosql.jdbc.PrestoDriver"));

        verify(service).list("tenant-a");
        verify(service).find("tenant-a", "artifact-001");
    }

    @Test
    void shouldReturnBadRequestWhenMultipartSizeExceedsConfiguredLimit() throws Exception {
        JdbcDriverArtifactApplicationService service = org.mockito.Mockito.mock(JdbcDriverArtifactApplicationService.class);
        when(service.upload(eq("tenant-a"), eq("HETU"), eq("io.prestosql.jdbc.PrestoDriver"), eq("351"), any()))
            .thenThrow(new MaxUploadSizeExceededException(60L * 1024L * 1024L));

        GovernanceDatasourceDriverProperties properties = new GovernanceDatasourceDriverProperties();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DatasourceDriverArtifactController(service))
            .setControllerAdvice(
                new DatasourceDriverUploadExceptionHandler(properties),
                new GlobalExceptionHandler()
            )
            .build();

        mockMvc.perform(multipart("/api/governance/datasource-drivers")
                .file(new MockMultipartFile("file", "too-large.jar", "application/java-archive", new byte[]{1, 2, 3}))
                .param("tenantId", "tenant-a")
                .param("engineType", "HETU")
                .param("driverClassName", "io.prestosql.jdbc.PrestoDriver")
                .param("versionLabel", "351"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(10001))
            .andExpect(jsonPath("$.message").value("驱动文件超过大小限制，请将 JDBC 驱动文件控制在 50 MB 以内后重试"));
    }

    @Test
    void shouldReturnBadRequestWhenUploadFilePartIsMissing() throws Exception {
        JdbcDriverArtifactApplicationService service = org.mockito.Mockito.mock(JdbcDriverArtifactApplicationService.class);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DatasourceDriverArtifactController(service))
            .setControllerAdvice(
                new DatasourceDriverUploadExceptionHandler(new GovernanceDatasourceDriverProperties()),
                new GlobalExceptionHandler()
            )
            .build();

        mockMvc.perform(multipart("/api/governance/datasource-drivers")
                .param("tenantId", "tenant-a")
                .param("engineType", "HETU")
                .param("driverClassName", "io.prestosql.jdbc.PrestoDriver")
                .param("versionLabel", "351"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(10001))
            .andExpect(jsonPath("$.message").value("请选择 JDBC 驱动 .jar 文件后再上传"));
    }

    private JdbcDriverArtifactVO sampleArtifact() {
        JdbcDriverArtifactVO response = new JdbcDriverArtifactVO();
        response.setArtifactId("artifact-001");
        response.setTenantId("tenant-a");
        response.setEngineType("HETU");
        response.setDriverClassName("io.prestosql.jdbc.PrestoDriver");
        response.setVersionLabel("351");
        response.setOriginalFileName("hetu-driver.jar");
        response.setSizeBytes(Long.valueOf(2048L));
        response.setSha256("abc123");
        response.setStatus("READY");
        response.setUploadedBy("admin-001");
        response.setCreatedAt(Instant.parse("2026-05-21T07:00:00Z"));
        response.setContractStage("LONG_TERM_BASELINE");
        response.setImplementationStage("JDBC_DRIVER_ARTIFACT_BASELINE");
        return response;
    }
}
