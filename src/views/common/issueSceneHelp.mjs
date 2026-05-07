const ISSUE_SCENE_TRANSLATIONS = {
  SELECT_STAR: {
    summary: 'SELECT * 风险',
    evidence: '查询返回所有列，容易放大网络、序列化和下游处理成本。',
    suggestedAction: '只选择业务实际需要的列，并确认宽表字段变更不会影响下游。'
  },
  SQL_SYNTAX_INVALID: {
    summary: 'SQL 语法无效',
    evidence: '结构解析器无法识别当前 SQL，后续 Access Parse 与治理判断不能完整展开。',
    suggestedAction: '先根据行、列、token 和短片段修正 SQL 语法后再重新解析。'
  },
  FULL_TABLE_SCAN_RISK: {
    summary: '全表扫描风险',
    evidence: '当前 SQL 缺少足够的过滤条件，可能触发大范围扫描。',
    suggestedAction: '补充分区键、时间范围或业务键过滤条件。'
  },
  LARGE_TABLE_JOIN_RISK: {
    summary: '大表 Join 风险',
    evidence: '当前 SQL 存在较重的关联路径，需要确认关联键与数据规模。',
    suggestedAction: '先核对 Join 键选择性，再结合 Access Parse 或压测确认。'
  },
  UNNECESSARY_SORT_RISK: {
    summary: '无必要排序风险',
    evidence: 'SQL 存在排序但缺少收敛条件，可能放大计算成本。',
    suggestedAction: '补充 LIMIT 或改为更适合下游使用的排序方式。'
  },
  REPEATED_EXPRESSION_RISK: {
    summary: '重复表达式计算风险',
    evidence: '表达式在多个位置重复出现，可能增加 CPU 开销。',
    suggestedAction: '抽取公共表达式或改写为 CTE / 中间层。'
  },
  LARGE_RESULT_SET_RISK: {
    summary: '结果集过大风险',
    evidence: '当前 SQL 可能返回过宽或过大的结果集。',
    suggestedAction: '显式指定列、过滤条件或 LIMIT。'
  },
  MISSING_QUERY_DATE_FILTER: {
    summary: '缺少查询日期过滤',
    evidence: '未识别到稳定的查询日期或分区日期条件，可能扩大扫描范围。',
    suggestedAction: '补充 dt、biz_date 或等价的日期范围过滤。'
  },
  SCALAR_SUBQUERY_IN_SELECT: {
    summary: 'SELECT 子句中的标量子查询',
    evidence: '投影列里存在标量子查询，可能造成重复执行。',
    suggestedAction: '改写为 Join、预聚合 CTE 或中间对象。'
  },
  NESTED_SUBQUERY_RISK: {
    summary: '嵌套子查询风险',
    evidence: 'SQL 存在多层嵌套查询，静态判断成本偏高。',
    suggestedAction: '拆成命名 CTE 分段检查。'
  },
  CORRELATED_SUBQUERY_RISK: {
    summary: '相关子查询风险',
    evidence: 'SQL 中存在相关引用，可能引入重复 lookup。',
    suggestedAction: '改写为显式 Join 或预计算阶段。'
  },
  FUNCTION_WRAPPED_PREDICATE: {
    summary: '函数包裹谓词风险',
    evidence: '过滤条件对列做了函数包裹，可能影响下推。',
    suggestedAction: '尽量改写成区间判断或归一化列比较。'
  },
  NOT_EXISTS_ANTI_JOIN_RISK: {
    summary: 'NOT EXISTS 反连接风险',
    evidence: '反连接语义需要额外确认空值与选择性行为。',
    suggestedAction: '确认语义后再考虑 staged rewrite。'
  },
  LEADING_WILDCARD_LIKE_RISK: {
    summary: '前缀通配 LIKE 风险',
    evidence: 'LIKE 以通配符开头，通常难以利用前缀裁剪。',
    suggestedAction: '使用可搜索键或前缀可裁剪谓词。'
  },
  OR_PREDICATE_INDEX_RISK: {
    summary: 'OR 谓词索引风险',
    evidence: 'WHERE 条件组合了多个 OR 分支，可能削弱裁剪能力。',
    suggestedAction: '评估 UNION ALL 或分段过滤是否更清晰。'
  },
  ORDER_BY_RANDOM_RISK: {
    summary: '随机排序风险',
    evidence: 'ORDER BY RAND / RANDOM 会引入昂贵的随机化与排序。',
    suggestedAction: '改用确定性采样或预生成样本键。'
  },
  REPEATED_TABLE_SCAN_RISK: {
    summary: '重复表扫描风险',
    evidence: '同一表在多个位置被重复引用，容易放大 IO / CPU。',
    suggestedAction: '通过 CTE 或中间对象预先分段。'
  },
  COMPLEX_QUERY_GRAPH_RISK: {
    summary: '复杂查询图风险',
    evidence: '当前查询图由多个 Join、谓词和子查询组成。',
    suggestedAction: '先拆分为可审查的阶段，再进入上线路径。'
  }
}

export function normalizeIssueSceneCode(value) {
  if (value && typeof value === 'object') {
    return String(value.riskCode || value.issueCode || value.issueScene || '').trim()
  }
  return String(value || '').trim()
}

export function issueSceneHelpText(value, chinese = false) {
  const code = normalizeIssueSceneCode(value)
  if (!code) {
    return ''
  }
  const translation = ISSUE_SCENE_TRANSLATIONS[code]
  if (!translation) {
    return chinese
      ? `${code}：结构解析命中的问题场景，请结合定位片段、逻辑对象和 Access Parse 状态判断治理优先级。`
      : `${code}: issue scene detected by structure parsing.`
  }
  if (chinese) {
    return `${translation.summary}：${translation.evidence} 建议：${translation.suggestedAction}`
  }
  return `${code}: ${translation.summary}`
}

export function riskDisplayText(risk, field, chinese = false) {
  const code = normalizeIssueSceneCode(risk)
  if (!chinese) {
    return risk?.[field] || '-'
  }
  return ISSUE_SCENE_TRANSLATIONS[code]?.[field] || risk?.[field] || '-'
}
