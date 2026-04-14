package com.sqlforge.backend.service;

import com.sqlforge.backend.web.dto.SqlIntentAnalysisRequest;
import com.sqlforge.backend.web.dto.SqlIntentBatchRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SqlIntentBatchAnalysisService {

    private final SqlIntentAnalysisService sqlIntentAnalysisService;

    public SqlIntentBatchAnalysisService(SqlIntentAnalysisService sqlIntentAnalysisService) {
        this.sqlIntentAnalysisService = sqlIntentAnalysisService;
    }

    public Map<String, Object> analyzeBatch(SqlIntentBatchRequest request) {
        List<SqlIntentAnalysisRequest.StatementInput> items = buildStatementInputs(
            request.getBatchId(),
            request.getSource(),
            request.getRawSqlText()
        );

        SqlIntentAnalysisRequest analysisRequest = new SqlIntentAnalysisRequest();
        analysisRequest.setStatements(items);
        Map<String, Object> analysis = sqlIntentAnalysisService.analyze(analysisRequest);

        Map<String, Object> result = new LinkedHashMap<String, Object>(analysis);
        result.put("batchId", request.getBatchId());
        result.put("source", request.getSource());
        result.put("parsedStatementCount", Integer.valueOf(items.size()));
        result.put("splitMode", resolveSplitMode(request.getRawSqlText(), items.size()));
        return result;
    }

    List<SqlIntentAnalysisRequest.StatementInput> buildStatementInputs(String batchId, String source, String rawSqlText) {
        List<String> statements = splitStatements(rawSqlText);
        List<SqlIntentAnalysisRequest.StatementInput> items = new ArrayList<SqlIntentAnalysisRequest.StatementInput>();

        for (int index = 0; index < statements.size(); index += 1) {
            SqlIntentAnalysisRequest.StatementInput item = new SqlIntentAnalysisRequest.StatementInput();
            item.setId(buildStatementId(batchId, index + 1));
            item.setSource(source);
            item.setSql(statements.get(index));
            items.add(item);
        }

        return items;
    }

    List<String> splitStatements(String rawSqlText) {
        List<String> statements = splitBySemicolon(rawSqlText);
        if (statements.size() <= 1) {
            List<String> fallback = splitByBlankLine(rawSqlText);
            if (fallback.size() > statements.size()) {
                statements = fallback;
            }
        }
        return statements;
    }

    private List<String> splitBySemicolon(String rawSqlText) {
        List<String> statements = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;

        for (int index = 0; index < rawSqlText.length(); index += 1) {
            char value = rawSqlText.charAt(index);
            char next = index + 1 < rawSqlText.length() ? rawSqlText.charAt(index + 1) : '\0';

            if (value == '\'' && !inDoubleQuote && !inBacktick) {
                current.append(value);
                if (inSingleQuote && next == '\'') {
                    current.append(next);
                    index += 1;
                } else {
                    inSingleQuote = !inSingleQuote;
                }
                continue;
            }

            if (value == '"' && !inSingleQuote && !inBacktick) {
                inDoubleQuote = !inDoubleQuote;
                current.append(value);
                continue;
            }

            if (value == '`' && !inSingleQuote && !inDoubleQuote) {
                inBacktick = !inBacktick;
                current.append(value);
                continue;
            }

            if (value == ';' && !inSingleQuote && !inDoubleQuote && !inBacktick) {
                appendIfNotEmpty(statements, current.toString());
                current.setLength(0);
                continue;
            }

            current.append(value);
        }

        appendIfNotEmpty(statements, current.toString());
        return statements;
    }

    private List<String> splitByBlankLine(String rawSqlText) {
        List<String> statements = new ArrayList<String>();
        String[] blocks = rawSqlText.split("(?:\\r?\\n){2,}");

        for (String block : blocks) {
            appendIfNotEmpty(statements, block);
        }

        return statements;
    }

    private void appendIfNotEmpty(List<String> statements, String candidate) {
        String normalized = normalizeCandidate(candidate);
        if (!normalized.isEmpty()) {
            statements.add(normalized);
        }
    }

    private String normalizeCandidate(String candidate) {
        String[] lines = candidate.split("\\r?\\n");
        StringBuilder builder = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("--") || trimmed.startsWith("#")) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(line);
        }

        return builder.toString().trim();
    }

    private String buildStatementId(String batchId, int index) {
        String prefix = batchId == null || batchId.trim().isEmpty() ? "batch" : batchId.trim();
        return prefix + "-" + String.format("%03d", Integer.valueOf(index));
    }

    private String resolveSplitMode(String rawSqlText, int parsedCount) {
        if (parsedCount <= 1) {
            return "single";
        }
        return rawSqlText.contains(";") ? "semicolon" : "blank-line";
    }
}
