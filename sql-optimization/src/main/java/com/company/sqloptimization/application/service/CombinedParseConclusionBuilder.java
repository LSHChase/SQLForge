package com.company.sqloptimization.application.service;

import com.company.sqloptimization.application.controller.vo.CombinedParseConclusionVO;
import com.company.sqloptimization.application.controller.vo.CombinedParseStatusVO;

final class CombinedParseConclusionBuilder {

    CombinedParseConclusionVO build(CombinedParseStatusVO status) {
        CombinedParseConclusionVO conclusion = new CombinedParseConclusionVO();
        boolean structureAvailable = status.getStructureParse() != null;
        boolean accessAvailable = status.getAccessParse() != null
            && "AVAILABLE".equals(status.getAccessParse().getServiceStatus())
            && "CONNECTED".equals(status.getAccessParse().getConnectionStatus());
        conclusion.setStructureAvailable(Boolean.valueOf(structureAvailable));
        conclusion.setAccessAvailable(Boolean.valueOf(accessAvailable));
        conclusion.setDegradeReason(status.getDegradeReason());
        if ("FAILED".equals(status.getStatus())) {
            conclusion.setOverallStatus("FAILED");
            conclusion.setSummary("结构解析返回失败级结果，因此访问解析证据不可用。");
            conclusion.setRecommendedAction("请先修复 SQL 语法或不受支持的结构问题，再重试解析。");
            return conclusion;
        }
        if ("PARTIAL_SUCCEEDED".equals(status.getStatus())) {
            return partialConclusion(status, conclusion);
        }
        if ("ACCESS_SUCCEEDED".equals(status.getStatus())) {
            conclusion.setOverallStatus("SUCCESS");
            conclusion.setSummary("结构解析与访问解析证据均已可用。");
            conclusion.setRecommendedAction("请将组合解析结果作为路由、优化和历史下钻的基线。");
            return conclusion;
        }
        conclusion.setOverallStatus("WAITING");
        conclusion.setSummary("结构解析已就绪，访问解析跟进流程仍在运行。");
        conclusion.setRecommendedAction("请轮询组合解析状态，直到访问解析进入终态。");
        return conclusion;
    }

    private CombinedParseConclusionVO partialConclusion(CombinedParseStatusVO status,
                                                        CombinedParseConclusionVO conclusion) {
        conclusion.setOverallStatus("PARTIAL_SUCCESS");
        if (status.getStructureParse() != null && !"VALID".equals(status.getStructureParse().getSyntaxStatus())) {
            conclusion.setSummary("结构解析失败，但仍存在其他解析证据通道。");
            conclusion.setRecommendedAction("请评审计划证据并修复 SQL 语法后，再重新运行完整解析。");
        } else {
            conclusion.setSummary("结构解析成功，但访问解析证据降级或不可用。");
            conclusion.setRecommendedAction("当前先使用结构证据，并在重新运行访问解析前检查数据源可用性。");
        }
        return conclusion;
    }
}
