package com.company.governance.application.controller;

import com.company.governance.application.controller.vo.JdbcDriverArtifactVO;
import com.company.governance.application.service.JdbcDriverArtifactApplicationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/governance/datasource-drivers")
public class DatasourceDriverArtifactController {

    private final JdbcDriverArtifactApplicationService service;

    public DatasourceDriverArtifactController(JdbcDriverArtifactApplicationService service) {
        this.service = service;
    }

    @PostMapping
    public JdbcDriverArtifactVO upload(@RequestParam("tenantId") String tenantId,
                                       @RequestParam("engineType") String engineType,
                                       @RequestParam("driverClassName") String driverClassName,
                                       @RequestParam("versionLabel") String versionLabel,
                                       @RequestParam("file") MultipartFile file) {
        return service.upload(tenantId, engineType, driverClassName, versionLabel, file);
    }

    @GetMapping
    public List<JdbcDriverArtifactVO> list(@RequestParam("tenantId") String tenantId) {
        return service.list(tenantId);
    }

    @GetMapping("/{artifactId}")
    public JdbcDriverArtifactVO find(@RequestParam("tenantId") String tenantId,
                                     @PathVariable("artifactId") String artifactId) {
        return service.find(tenantId, artifactId);
    }
}
