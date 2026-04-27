package com.company.governance.application.service;

import com.company.governance.application.controller.vo.DatabaseViewDependencyVO;
import com.company.governance.application.controller.vo.DatabaseViewRefVO;
import com.company.governance.domain.dbview.entity.DatabaseViewDependency;
import com.company.governance.domain.dbview.entity.DatabaseViewRef;
import com.company.governance.domain.dbview.repository.DatabaseViewDependencyRepository;
import com.company.governance.domain.dbview.repository.DatabaseViewRepository;
import com.company.governance.domain.tenant.logic.TenantAccessLogic;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.context.TenantContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.GovernanceDbViewDependencyRef;
import com.company.sqlforge.common.governance.GovernanceDbViewResolveRequest;
import com.company.sqlforge.common.governance.GovernanceDbViewResolveResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DatabaseViewCatalogApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseViewCatalogApplicationService.class);
    private static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";

    private final DatabaseViewRepository databaseViewRepository;
    private final DatabaseViewDependencyRepository databaseViewDependencyRepository;
    private final TenantAccessLogic tenantAccessLogic;

    public DatabaseViewCatalogApplicationService(DatabaseViewRepository databaseViewRepository,
                                                 DatabaseViewDependencyRepository databaseViewDependencyRepository,
                                                 TenantAccessLogic tenantAccessLogic) {
        this.databaseViewRepository = databaseViewRepository;
        this.databaseViewDependencyRepository = databaseViewDependencyRepository;
        this.tenantAccessLogic = tenantAccessLogic;
    }

    public List<DatabaseViewRefVO> listDbViews(String tenantId, String datasourceCode) {
        String effectiveTenantId = resolveAndAuthorizeTenant(tenantId, datasourceCode);
        List<DatabaseViewRef> refs = databaseViewRepository.findByTenantAndDatasource(effectiveTenantId, trimToNull(datasourceCode));
        List<DatabaseViewRefVO> items = new ArrayList<DatabaseViewRefVO>();
        for (DatabaseViewRef ref : refs) {
            items.add(toVo(ref));
        }
        return items;
    }

    public DatabaseViewRefVO findDbView(String tenantId, String datasourceCode, String viewName) {
        String effectiveTenantId = resolveAndAuthorizeTenant(tenantId, datasourceCode);
        DatabaseViewRef ref = databaseViewRepository.findByTenantDatasourceAndViewName(
            effectiveTenantId,
            requireText(datasourceCode, "datasourceCode"),
            requireText(viewName, "viewName")
        ).orElseThrow(() -> new BizException(
            ErrorCodeConstants.SYSTEM_RESOURCE_NOT_FOUND,
            HttpStatus.NOT_FOUND,
            "Database view not found"
        ));
        return toVo(ref);
    }

    public GovernanceDbViewResolveResponse resolveDbView(GovernanceDbViewResolveRequest request) {
        String effectiveTenantId = resolveAndAuthorizeTenant(request == null ? null : request.getTenantId(),
            request == null ? null : request.getDatasourceCode());
        String datasourceCode = requireText(request == null ? null : request.getDatasourceCode(), "datasourceCode");
        String viewName = requireText(request == null ? null : request.getViewName(), "viewName");
        GovernanceDbViewResolveResponse response = new GovernanceDbViewResolveResponse();
        response.setTenantId(effectiveTenantId);
        response.setDatasourceCode(datasourceCode);
        response.setViewName(viewName);
        DatabaseViewRef ref = databaseViewRepository.findByTenantDatasourceAndViewName(effectiveTenantId, datasourceCode, viewName)
            .orElse(null);
        if (ref == null) {
            response.setResolved(Boolean.FALSE);
            response.setDependencies(Collections.<GovernanceDbViewDependencyRef>emptyList());
            return response;
        }
        response.setObjectKey(ref.getObjectKey());
        response.setResolved(Boolean.TRUE);
        response.setDependencies(toGovernanceDependencies(effectiveTenantId, ref.getId()));
        LOGGER.info("Resolved DB view dependency evidence, tenantId={}, datasourceCode={}, viewName={}, dependencyCount={}, traceId={}",
            effectiveTenantId,
            datasourceCode,
            viewName,
            Integer.valueOf(response.getDependencies().size()),
            RequestContext.getTraceId());
        return response;
    }

    private String resolveAndAuthorizeTenant(String tenantId, String datasourceCode) {
        String currentTenantId = TenantContext.get();
        boolean platformAdmin = RequestContext.hasRole(PLATFORM_ADMIN);
        if (!StringUtils.hasText(currentTenantId)) {
            throw new BizException(ErrorCodeConstants.SYSTEM_CONTEXT_MISSING, HttpStatus.UNAUTHORIZED, "Tenant context is missing");
        }
        String effectiveTenantId = StringUtils.hasText(tenantId) ? tenantId : currentTenantId;
        if (!platformAdmin && !currentTenantId.equals(effectiveTenantId)) {
            throw new BizException(
                ErrorCodeConstants.GOVERNANCE_TENANT_ACCESS_DENIED,
                HttpStatus.FORBIDDEN,
                ErrorCodeConstants.GOVERNANCE_TENANT_ACCESS_DENIED_MESSAGE
            );
        }
        if (!platformAdmin && StringUtils.hasText(datasourceCode)
            && !tenantAccessLogic.validateDataSourceAccess(currentTenantId, datasourceCode)) {
            throw new BizException(
                ErrorCodeConstants.GOVERNANCE_DATASOURCE_ACCESS_DENIED,
                HttpStatus.FORBIDDEN,
                ErrorCodeConstants.GOVERNANCE_DATASOURCE_ACCESS_DENIED_MESSAGE
            );
        }
        return effectiveTenantId;
    }

    private DatabaseViewRefVO toVo(DatabaseViewRef ref) {
        DatabaseViewRefVO vo = new DatabaseViewRefVO();
        vo.setViewId(ref.getId());
        vo.setTenantId(ref.getTenantId());
        vo.setDatasourceCode(ref.getDatasourceCode());
        vo.setViewName(ref.getViewName());
        vo.setObjectKey(ref.getObjectKey());
        vo.setSchemaName(ref.getSchemaName());
        vo.setCatalogName(ref.getCatalogName());
        vo.setOwnerUser(ref.getOwnerUser());
        vo.setQueryable(ref.getQueryable());
        vo.setDependencies(toDependencies(ref.getTenantId(), ref.getId()));
        return vo;
    }

    private List<DatabaseViewDependencyVO> toDependencies(String tenantId, String dbViewId) {
        List<DatabaseViewDependency> dependencies = databaseViewDependencyRepository.findByTenantAndDbViewId(tenantId, dbViewId);
        List<DatabaseViewDependencyVO> items = new ArrayList<DatabaseViewDependencyVO>();
        for (DatabaseViewDependency dependency : dependencies) {
            DatabaseViewDependencyVO item = new DatabaseViewDependencyVO();
            item.setDependencyObjectType(dependency.getDependencyObjectType());
            item.setDependencyObjectKey(dependency.getDependencyObjectKey());
            item.setDependencyObjectName(dependency.getDependencyObjectName());
            items.add(item);
        }
        return items;
    }

    private List<GovernanceDbViewDependencyRef> toGovernanceDependencies(String tenantId, String dbViewId) {
        List<DatabaseViewDependency> dependencies = databaseViewDependencyRepository.findByTenantAndDbViewId(tenantId, dbViewId);
        List<GovernanceDbViewDependencyRef> items = new ArrayList<GovernanceDbViewDependencyRef>();
        for (DatabaseViewDependency dependency : dependencies) {
            GovernanceDbViewDependencyRef item = new GovernanceDbViewDependencyRef();
            item.setObjectType(dependency.getDependencyObjectType());
            item.setObjectKey(dependency.getDependencyObjectKey());
            item.setObjectName(dependency.getDependencyObjectName());
            items.add(item);
        }
        return items;
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT, HttpStatus.BAD_REQUEST, fieldName + " must not be empty");
        }
        return value;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
