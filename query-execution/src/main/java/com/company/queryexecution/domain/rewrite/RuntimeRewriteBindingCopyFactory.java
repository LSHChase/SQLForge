package com.company.queryexecution.domain.rewrite;

final class RuntimeRewriteBindingCopyFactory {

    private RuntimeRewriteBindingCopyFactory() {
    }

    static RuntimeRewriteBinding.Builder copy(RuntimeRewriteBinding binding) {
        return RuntimeRewriteBinding.builder()
            .runtimeBindingId(binding.getRuntimeBindingId())
            .tenantId(binding.getTenantId())
            .rewriteRecordId(binding.getRewriteRecordId())
            .recommendationId(binding.getRecommendationId())
            .sourceType(binding.getSourceType())
            .sourceKind(binding.getSourceKind())
            .sourceId(binding.getSourceId())
            .sqlFingerprint(binding.getSqlFingerprint())
            .originalSqlDigest(binding.getOriginalSqlDigest())
            .originalSqlText(binding.getOriginalSqlText())
            .recommendedSqlText(binding.getRecommendedSqlText())
            .rewriteMatchMode(binding.getRewriteMatchMode())
            .rewriteProgramJson(binding.getRewriteProgramJson())
            .templateFamilyFingerprint(binding.getTemplateFamilyFingerprint())
            .runtimeMatchObjectRefs(binding.getRuntimeMatchObjectRefs())
            .runtimeMatchObjectNames(binding.getRuntimeMatchObjectNames())
            .analysisPhysicalObjectRefs(binding.getAnalysisPhysicalObjectRefs())
            .metadataSnapshotVersion(binding.getMetadataSnapshotVersion())
            .viewDefinitionHash(binding.getViewDefinitionHash())
            .metadataDegradationReason(binding.getMetadataDegradationReason())
            .datasourceCode(binding.getDatasourceCode())
            .status(binding.getStatus())
            .ruleVersion(binding.getRuleVersion())
            .runtimeRuleVersion(binding.getRuntimeRuleVersion())
            .activatedBy(binding.getActivatedBy())
            .activatedAt(binding.getActivatedAt())
            .pausedBy(binding.getPausedBy())
            .pausedAt(binding.getPausedAt())
            .pauseReason(binding.getPauseReason())
            .createdAt(binding.getCreatedAt())
            .updatedAt(binding.getUpdatedAt());
    }
}
