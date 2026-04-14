import { createHash } from 'node:crypto';

function normalizeSql(sql) {
  return sql.trim().replace(/\s+/g, ' ');
}

function hash(value) {
  return createHash('md5').update(value).digest('hex');
}

function detectTables(sql) {
  const tables = new Set();
  const regex = /\b(?:from|join)\s+([a-zA-Z0-9_.]+)/gi;

  for (const match of sql.matchAll(regex)) {
    tables.add(match[1]);
  }

  return [...tables];
}

function detectTableReferences(sql) {
  const references = [];
  const regex = /\b(?:from|join)\s+([a-zA-Z0-9_.]+)/gi;

  for (const match of sql.matchAll(regex)) {
    references.push(match[1]);
  }

  return references;
}

function detectScanPattern(sql) {
  if (!/\bwhere\b/i.test(sql)) {
    return 'full-table';
  }

  if (/\bbetween\b|\b>\b|\b<\b|\b>=\b|\b<=\b/i.test(sql)) {
    return 'range';
  }

  return 'point-lookup';
}

function detectJoinType(joinCount) {
  if (joinCount === 0) {
    return 'single-table';
  }

  if (joinCount === 1) {
    return 'star';
  }

  if (joinCount <= 3) {
    return 'chain';
  }

  return 'snowflake';
}

function detectComputeDensity(sql) {
  if (/\bover\s*\(/i.test(sql)) {
    return 'window';
  }

  if (/\b(sum|avg|max|min|count)\s*\(/i.test(sql)) {
    return 'heavy-aggregation';
  }

  if (/\bregexp_|json_|map_|array_/i.test(sql)) {
    return 'udf-heavy';
  }

  return 'light';
}

function detectGroupBy(sql) {
  return /\bgroup\s+by\b/i.test(sql);
}

function detectRepeatedTables(tableReferences) {
  const counts = new Map();

  for (const table of tableReferences) {
    counts.set(table, (counts.get(table) ?? 0) + 1);
  }

  return [...counts.entries()]
    .filter(([, count]) => count > 1)
    .map(([table, count]) => ({ table, count }));
}

function collectSelectExpressions(sql) {
  const match = sql.match(/\bselect\s+(.+?)\s+from\b/i);

  if (!match) {
    return [];
  }

  return match[1]
    .split(',')
    .map((expression) => expression.trim())
    .filter(Boolean);
}

function detectRepeatedExpressions(sql) {
  const expressions = collectSelectExpressions(sql);
  const normalized = expressions
    .filter((expression) => /\(/.test(expression))
    .map((expression) => expression.replace(/\s+/g, ' ').toLowerCase());
  const counts = new Map();

  for (const expression of normalized) {
    counts.set(expression, (counts.get(expression) ?? 0) + 1);
  }

  return [...counts.entries()]
    .filter(([, count]) => count > 1)
    .map(([expression, count]) => ({ expression, count }));
}

function detectPredicateFunctionWrapping(sql) {
  return /\bwhere\b.+\b(?:lower|upper|date|cast|substr|substring|coalesce)\s*\(/i.test(sql);
}

function detectResultSetRisk({ sql, hasLimit, hasGroupBy }) {
  if (hasLimit) {
    return 'bounded';
  }

  if (hasGroupBy || /\bcount\s*\(/i.test(sql)) {
    return 'aggregated';
  }

  return 'wide-open';
}

function detectJoinConditions(sql) {
  const conditions = [];
  const regex = /\bon\s+([a-zA-Z0-9_]+)\.([a-zA-Z0-9_]+)\s*=\s*([a-zA-Z0-9_]+)\.([a-zA-Z0-9_]+)/gi;

  for (const match of sql.matchAll(regex)) {
    conditions.push({
      leftAlias: match[1],
      leftKey: match[2],
      rightAlias: match[3],
      rightKey: match[4]
    });
  }

  return conditions;
}

function buildValidations({ sql, scanPattern, hasOrderBy, hasSelectStar, hasLimit, joinConditions, repeatedTables, repeatedExpressions, resultSetRisk }) {
  const predicatePushdownRisk = scanPattern === 'full-table' || detectPredicateFunctionWrapping(sql);
  const bucketJoinCandidate = joinConditions.length > 0;

  return {
    predicatePushdown: {
      status: predicatePushdownRisk ? 'warn' : 'pass',
      message: predicatePushdownRisk ? '过滤条件可能无法有效下推，需要验证分区裁剪与表达式包裹' : '过滤条件具备较好的下推机会'
    },
    bucketJoin: {
      status: joinConditions.length === 0 ? 'na' : bucketJoinCandidate ? 'opportunity' : 'warn',
      message:
        joinConditions.length === 0
          ? '当前 SQL 无 Join，不涉及分桶 Join'
          : bucketJoinCandidate
            ? '存在相同 Join Key，可验证 Bucket Join 与 Broadcast 策略'
            : 'Join Key 形态较复杂，需要重点检查 Shuffle'
    },
    expressionReuse: {
      status: repeatedExpressions.length > 0 ? 'warn' : 'pass',
      message: repeatedExpressions.length > 0 ? '检测到重复函数表达式，可考虑复用或物化' : '未检测到明显重复表达式'
    },
    repeatedJoin: {
      status: repeatedTables.length > 0 ? 'warn' : 'pass',
      message: repeatedTables.length > 0 ? '同表被多次关联，可考虑 CTE 或临时表物化' : '未检测到重复关联同表'
    },
    sortNecessity: {
      status: hasOrderBy && !hasLimit ? 'warn' : hasOrderBy ? 'observe' : 'pass',
      message:
        hasOrderBy && !hasLimit
          ? 'ORDER BY 未配合 LIMIT，需验证是否为非必要排序'
          : hasOrderBy
            ? '存在 ORDER BY，但结果集有界，建议结合业务语义确认必要性'
            : '未检测到显著排序风险'
    },
    projectionPushdown: {
      status: hasSelectStar ? 'warn' : 'pass',
      message: hasSelectStar ? 'SELECT * 可能导致列裁剪不足' : '查询具备较好的投影下推条件'
    },
    resultSetControl: {
      status: resultSetRisk === 'wide-open' ? 'warn' : 'pass',
      message: resultSetRisk === 'wide-open' ? '结果集无明显边界，需防止返回过大结果' : '结果集规模具备一定控制'
    }
  };
}

function detectResourceType({ scanPattern, joinCount, computeDensity, hasOrderBy }) {
  if (scanPattern === 'full-table') {
    return 'io-bound';
  }

  if (joinCount >= 2) {
    return 'network-mixed';
  }

  if (computeDensity !== 'light' || hasOrderBy) {
    return 'cpu-bound';
  }

  return 'memory-mixed';
}

function buildRisks({
  sql,
  scanPattern,
  joinCount,
  hasOrderBy,
  hasSelectStar,
  hasLimit,
  tables,
  computeDensity,
  repeatedTables,
  repeatedExpressions,
  resultSetRisk
}) {
  const risks = [];

  if (scanPattern === 'full-table') {
    risks.push({
      code: 'FULL_SCAN',
      severity: 'high',
      message: '查询缺少有效过滤条件，存在全表扫描风险'
    });
  }

  if (joinCount >= 2) {
    risks.push({
      code: 'COMPLEX_JOIN',
      severity: joinCount >= 4 ? 'high' : 'medium',
      message: 'Join 链较长，存在 Shuffle 放大和计划脆弱风险'
    });
  }

  if (/\bjoin\b/i.test(sql) && /\bon\b/i.test(sql) === false) {
    risks.push({
      code: 'MISSING_JOIN_CONDITION',
      severity: 'critical',
      message: '疑似缺失 Join 条件，需排查 Cartesian Product'
    });
  }

  if (hasOrderBy && !hasLimit) {
    risks.push({
      code: 'UNBOUNDED_SORT',
      severity: 'medium',
      message: 'ORDER BY 未配合 LIMIT，可能触发非必要全局排序'
    });
  }

  if (hasSelectStar) {
    risks.push({
      code: 'SELECT_STAR',
      severity: 'medium',
      message: 'SELECT * 可能导致投影下推失效与网络放大'
    });
  }

  if (tables.length >= 3) {
    risks.push({
      code: 'MULTI_TABLE_TOUCH',
      severity: 'medium',
      message: '涉及多表访问，建议重点验证统计信息与分桶策略'
    });
  }

  if (computeDensity === 'window' || computeDensity === 'udf-heavy') {
    risks.push({
      code: 'HEAVY_COMPUTE',
      severity: 'medium',
      message: '存在窗口函数或高复杂表达式，需关注 CPU 与内存峰值'
    });
  }

  if (repeatedTables.length > 0) {
    risks.push({
      code: 'REPEATED_TABLE_JOIN',
      severity: 'medium',
      message: '检测到同表多次关联，可能需要 CTE 或物化来降低重复计算'
    });
  }

  if (repeatedExpressions.length > 0) {
    risks.push({
      code: 'REPEATED_EXPRESSION',
      severity: 'medium',
      message: '检测到重复表达式计算，可能存在 CPU 浪费'
    });
  }

  if (resultSetRisk === 'wide-open') {
    risks.push({
      code: 'LARGE_RESULT_SET',
      severity: 'medium',
      message: '查询结果无聚合或 LIMIT 约束，可能导致结果集过大'
    });
  }

  return risks;
}

function estimateResources({ scanPattern, joinCount, computeDensity, tables, risks }) {
  const score =
    (scanPattern === 'full-table' ? 40 : scanPattern === 'range' ? 20 : 10) +
    joinCount * 8 +
    (computeDensity === 'light' ? 5 : computeDensity === 'heavy-aggregation' ? 18 : 24) +
    tables.length * 3 +
    risks.length * 5;

  return {
    score,
    cpu: score >= 80 ? 'high' : score >= 45 ? 'medium' : 'low',
    memory: joinCount >= 2 || computeDensity !== 'light' ? 'medium-high' : 'medium',
    io: scanPattern === 'full-table' ? 'high' : scanPattern === 'range' ? 'medium' : 'low',
    network: joinCount >= 2 ? 'medium-high' : 'low'
  };
}

export class QueryIntentEngine {
  constructor(defaultSlaMs) {
    this.defaultSlaMs = defaultSlaMs;
  }

  assess(sql, options = {}) {
    if (!sql || typeof sql !== 'string') {
      throw new Error('sql is required');
    }

    const normalizedSql = normalizeSql(sql);
    const tables = detectTables(normalizedSql);
    const tableReferences = detectTableReferences(normalizedSql);
    const joinCount = [...normalizedSql.matchAll(/\bjoin\b/gi)].length;
    const hasOrderBy = /\border\s+by\b/i.test(normalizedSql);
    const hasLimit = /\blimit\b/i.test(normalizedSql);
    const hasGroupBy = detectGroupBy(normalizedSql);
    const hasSelectStar = /\bselect\s+\*/i.test(normalizedSql);
    const scanPattern = detectScanPattern(normalizedSql);
    const computeDensity = detectComputeDensity(normalizedSql);
    const repeatedTables = detectRepeatedTables(tableReferences);
    const repeatedExpressions = detectRepeatedExpressions(normalizedSql);
    const resultSetRisk = detectResultSetRisk({
      sql: normalizedSql,
      hasLimit,
      hasGroupBy
    });
    const joinConditions = detectJoinConditions(normalizedSql);
    const validations = buildValidations({
      sql: normalizedSql,
      scanPattern,
      hasOrderBy,
      hasSelectStar,
      hasLimit,
      joinConditions,
      repeatedTables,
      repeatedExpressions,
      resultSetRisk
    });
    const risks = buildRisks({
      sql: normalizedSql,
      scanPattern,
      joinCount,
      hasOrderBy,
      hasSelectStar,
      hasLimit,
      tables,
      computeDensity,
      repeatedTables,
      repeatedExpressions,
      resultSetRisk
    });
    const resourceEstimate = estimateResources({
      scanPattern,
      joinCount,
      computeDensity,
      tables,
      risks
    });

    return {
      fingerprint: hash(normalizedSql),
      normalizedSql,
      tables,
      signals: {
        joinCount,
        hasOrderBy,
        hasLimit,
        hasGroupBy,
        hasSelectStar,
        repeatedTables,
        repeatedExpressions,
        resultSetRisk,
        joinConditions
      },
      labels: {
        scanPattern,
        joinType: detectJoinType(joinCount),
        computeDensity,
        resourceType: detectResourceType({
          scanPattern,
          joinCount,
          computeDensity,
          hasOrderBy
        }),
        slaTier: (options.slaMs ?? this.defaultSlaMs) <= 3000 ? 'interactive' : 'reporting'
      },
      validations,
      risks,
      resourceEstimate
    };
  }
}
