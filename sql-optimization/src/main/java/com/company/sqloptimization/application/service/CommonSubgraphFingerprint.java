package com.company.sqloptimization.application.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlJoin;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOrderBy;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.SqlWith;
import org.apache.calcite.sql.SqlWithItem;
import org.springframework.util.StringUtils;

final class CommonSubgraphFingerprint {

    private static final int CACHE_MAX_SIZE = 1024;
    private static final Map<String, String> CACHE =
        Collections.synchronizedMap(new LinkedHashMap<String, String>(CACHE_MAX_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                return size() > CACHE_MAX_SIZE;
            }
        });

    private CommonSubgraphFingerprint() {
    }

    static String subgraphFingerprint(String sql) {
        String key = CommonSubgraphSqlText.trimTrailingSemicolon(sql);
        String cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        String fingerprint = shortSha256(canonicalSql(key));
        CACHE.put(key, fingerprint);
        return fingerprint;
    }

    static String canonicalSql(String sql) {
        try {
            SqlNode parsed = CommonSubgraphSqlParser.parseStatement(sql);
            String canonical = canonicalSqlText(parsed.toString());
            LinkedHashMap<String, String> relationAliases = relationAliasReplacements(parsed);
            List<String> aliases = new ArrayList<String>(relationAliases.keySet());
            Collections.sort(aliases, new Comparator<String>() {
                @Override
                public int compare(String left, String right) {
                    return Integer.compare(right.length(), left.length());
                }
            });
            for (String alias : aliases) {
                canonical = replaceRelationAliasReferences(canonical, alias, relationAliases.get(alias));
            }
            return canonical;
        } catch (RuntimeException ex) {
            return canonicalSqlText(sql);
        }
    }

    static String canonicalSqlText(String sql) {
        return CommonSubgraphSqlText.trimTrailingSemicolon(sql)
            .replace("`", "")
            .replace("\"", "")
            .replace('\n', ' ')
            .replace('\r', ' ')
            .trim()
            .replaceAll("\\s+", " ")
            .toUpperCase(java.util.Locale.ROOT);
    }

    static String shortSha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((value == null ? "" : value).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < bytes.length && i < 8; i++) {
                String hex = Integer.toHexString(bytes[i] & 0xff);
                if (hex.length() == 1) {
                    builder.append('0');
                }
                builder.append(hex);
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            return Integer.toHexString((value == null ? "" : value).hashCode());
        }
    }

    private static LinkedHashMap<String, String> relationAliasReplacements(SqlNode node) {
        LinkedHashSet<String> aliases = new LinkedHashSet<String>();
        collectRelationAliases(node, aliases);
        LinkedHashMap<String, String> replacements = new LinkedHashMap<String, String>();
        int index = 0;
        for (String alias : aliases) {
            String normalized = canonicalSqlText(CommonSubgraphSqlText.cleanIdentifier(alias));
            if (!StringUtils.hasText(normalized) || replacements.containsKey(normalized)) {
                continue;
            }
            replacements.put(normalized, "__REL_ALIAS_" + index + "__");
            index++;
        }
        return replacements;
    }

    private static void collectRelationAliases(SqlNode node, Set<String> aliases) {
        if (node == null) {
            return;
        }
        if (node instanceof SqlNodeList) {
            for (SqlNode item : ((SqlNodeList) node).getList()) {
                collectRelationAliases(item, aliases);
            }
            return;
        }
        if (node instanceof SqlOrderBy) {
            collectRelationAliases(((SqlOrderBy) node).query, aliases);
            collectRelationAliases(((SqlOrderBy) node).orderList, aliases);
            return;
        }
        if (node instanceof SqlWith) {
            SqlWith with = (SqlWith) node;
            if (with.withList != null) {
                for (SqlNode item : with.withList.getList()) {
                    if (item instanceof SqlWithItem) {
                        SqlWithItem withItem = (SqlWithItem) item;
                        if (withItem.name != null) {
                            CommonSubgraphSqlText.addIfText(aliases, withItem.name.toString());
                        }
                        collectRelationAliases(withItem.query, aliases);
                    }
                }
            }
            collectRelationAliases(with.body, aliases);
            return;
        }
        if (node instanceof SqlSelect) {
            SqlSelect select = (SqlSelect) node;
            collectRelationAliasesFrom(select.getFrom(), aliases);
            collectRelationAliases(select.getWhere(), aliases);
            collectRelationAliases(select.getHaving(), aliases);
            collectRelationAliases(select.getSelectList(), aliases);
            collectRelationAliases(select.getOrderList(), aliases);
            return;
        }
        if (node instanceof SqlCall) {
            for (SqlNode operand : ((SqlCall) node).getOperandList()) {
                collectRelationAliases(operand, aliases);
            }
        }
    }

    private static void collectRelationAliasesFrom(SqlNode from, Set<String> aliases) {
        if (from == null) {
            return;
        }
        if (from instanceof SqlJoin) {
            collectRelationAliasesFrom(((SqlJoin) from).getLeft(), aliases);
            collectRelationAliasesFrom(((SqlJoin) from).getRight(), aliases);
            return;
        }
        if (from instanceof SqlBasicCall && from.getKind() == SqlKind.AS) {
            List<SqlNode> operands = ((SqlBasicCall) from).getOperandList();
            if (operands.size() >= 2) {
                CommonSubgraphSqlText.addIfText(aliases, operands.get(1).toString());
                collectRelationAliases(operands.get(0), aliases);
            }
            return;
        }
        collectRelationAliases(from, aliases);
    }

    private static String replaceRelationAliasReferences(String canonicalSql, String alias, String replacement) {
        if (!StringUtils.hasText(canonicalSql)
            || !StringUtils.hasText(alias)
            || !StringUtils.hasText(replacement)) {
            return canonicalSql;
        }
        String quotedAlias = Pattern.quote(alias);
        String result = canonicalSql.replaceAll(
            "(?<![\\p{L}\\p{N}_$])" + quotedAlias + "\\s*\\.",
            replacement + "."
        );
        return result.replaceAll(
            "(?i)(\\bAS\\s+)" + quotedAlias + "(?![\\p{L}\\p{N}_$])",
            "$1" + replacement
        );
    }
}
