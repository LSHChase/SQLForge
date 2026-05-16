package com.company.governance.application.controller;

import com.company.governance.application.controller.vo.MetadataSchemaAssetVO;
import com.company.governance.application.controller.vo.MetadataTableAssetVO;
import com.company.governance.application.service.MetadataAssetCatalogApplicationService;
import com.company.sqlforge.common.context.RequestContext;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/governance/metadata")
public class MetadataAssetCatalogController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataAssetCatalogController.class);

    private final MetadataAssetCatalogApplicationService metadataAssetCatalogApplicationService;

    public MetadataAssetCatalogController(MetadataAssetCatalogApplicationService metadataAssetCatalogApplicationService) {
        this.metadataAssetCatalogApplicationService = metadataAssetCatalogApplicationService;
    }

    @GetMapping("/schemas")
    public List<MetadataSchemaAssetVO> listSchemas(@RequestParam(value = "tenantId", required = false) String tenantId,
                                                   @RequestParam(value = "datasourceCode", required = false) String datasourceCode) {
        LOGGER.info("处理元数据 schema 列表查询，tenantId={}, datasourceCode={}, traceId={}",
            tenantId, datasourceCode, RequestContext.getTraceId());
        return metadataAssetCatalogApplicationService.listSchemas(tenantId, datasourceCode);
    }

    @GetMapping("/schemas/{schemaName}")
    public MetadataSchemaAssetVO getSchema(@PathVariable("schemaName") String schemaName,
                                           @RequestParam(value = "tenantId", required = false) String tenantId,
                                           @RequestParam(value = "datasourceCode", required = false) String datasourceCode) {
        return metadataAssetCatalogApplicationService.findSchema(tenantId, datasourceCode, schemaName);
    }

    @GetMapping("/tables")
    public List<MetadataTableAssetVO> listTables(@RequestParam(value = "tenantId", required = false) String tenantId,
                                                 @RequestParam(value = "datasourceCode", required = false) String datasourceCode,
                                                 @RequestParam(value = "schemaName", required = false) String schemaName) {
        LOGGER.info("处理元数据表列表查询，tenantId={}, datasourceCode={}, schemaName={}, traceId={}",
            tenantId, datasourceCode, schemaName, RequestContext.getTraceId());
        return metadataAssetCatalogApplicationService.listTables(tenantId, datasourceCode, schemaName);
    }

    @GetMapping("/tables/{tableName}")
    public MetadataTableAssetVO getTable(@PathVariable("tableName") String tableName,
                                         @RequestParam(value = "tenantId", required = false) String tenantId,
                                         @RequestParam(value = "datasourceCode", required = false) String datasourceCode,
                                         @RequestParam(value = "schemaName", required = false) String schemaName) {
        return metadataAssetCatalogApplicationService.findTable(tenantId, datasourceCode, schemaName, tableName);
    }
}
