const MV_TYPE_DETAILS = {
  PARAMETERIZED_AGG_MV: {
    title: '参数外提聚合 MV',
    description: '把可变查询参数提升为 MV 维度，rewrite 再二次过滤或二次聚合。'
  },
  PREJOIN_MV: {
    title: '预连接宽表 MV',
    description: '物化稳定 INNER 等值 Join 后的明细宽表，rewrite 避开原始 Join 图。'
  },
  STAR_AGG_MV: {
    title: '星型聚合 MV',
    description: '围绕事实表与维表 Join 图物化聚合结果，支持维度切片复用。'
  },
  ROLLUP_MV: {
    title: '时间上卷 MV',
    description: '按更细自然时间粒度物化，rewrite 从日粒度上卷到月、季或年。'
  },
  COMMON_SUBGRAPH_MV: {
    title: '公共子图 MV',
    description: '物化 CTE、派生表或跨 SQL 共享子图，rewrite 引用该共享子图产物。'
  }
}

const STATUS_DETAILS = {
  GENERATED: {
    label: '已生成 SQL 产物',
    description: '仅表示 SQLForge 生成了可审查的 MV SQL 五件套，不代表 MV 已创建、已刷新、已验证、已审批或 runtime 已生效。'
  },
  BLOCKED: {
    label: '已阻断',
    description: '当前产物不应进入发布链路；需要先处理 blockingReasons 或 blockedPredicates。'
  },
  REVIEW_REQUIRED: {
    label: '需要人工复核',
    description: '当前产物只能进入人工复核，不能自动发布；reviewWarnings 必须被单独阅读。'
  }
}

const COVERAGE_LABELS = [
  ['coversProjection', '投影覆盖'],
  ['coversFilters', '过滤覆盖'],
  ['coversGrouping', '分组覆盖'],
  ['coversMeasures', '指标覆盖'],
  ['coversSecurity', '安全谓词覆盖'],
  ['rewriteSqlReadonly', 'rewrite 只读'],
  ['rewriteSqlReferencesMv', 'rewrite 引用 MV'],
  ['rewriteSqlAvoidsOriginalSources', 'rewrite 避开原表']
]

const SQL_BLOCKS = [
  ['ddlSql', '建表 SQL'],
  ['refreshSql', '刷新 SQL'],
  ['validationSql', '验证 SQL'],
  ['rollbackSql', '回滚 SQL'],
  ['rewriteSql', 'MV rewrite SQL']
]

const PREDICATE_GROUPS = [
  ['externalizedPredicates', '外提谓词'],
  ['retainedPredicates', '保留谓词'],
  ['securityPredicates', '安全谓词'],
  ['blockedPredicates', '阻断谓词']
]

const DETAIL_SECTION_KEYS = [
  ['joinKeys', 'Join Key 证据'],
  ['fieldMappings', '字段映射证据'],
  ['factTable', '事实表证据'],
  ['dimensionTables', '维表证据'],
  ['dimensionSources', '维度来源证据'],
  ['measureSources', '指标来源证据'],
  ['rowAmplificationRisk', '行数放大复核证据'],
  ['timeRollupEvidence', '时间上卷证据'],
  ['starSchemaEvidence', '星型模型证据'],
  ['commonSubgraphEvidence', '公共子图证据']
]

const hasValue = value => value !== null && value !== undefined && String(value).trim() !== ''

const firstValue = (...values) => values.find(hasValue)

const asArray = value => {
  if (Array.isArray(value)) {
    return value
  }
  if (hasValue(value)) {
    return [value]
  }
  return []
}

const displayValue = value => {
  if (Array.isArray(value)) {
    return value.length ? value.map(displayValue).join(' / ') : '-'
  }
  if (value && typeof value === 'object') {
    const summary = summarizeObject(value)
    return summary || JSON.stringify(value)
  }
  return hasValue(value) ? String(value) : '-'
}

const boolDisplay = value => {
  if (value === true) {
    return 'true'
  }
  if (value === false) {
    return 'false'
  }
  return '-'
}

const field = (key, label, value, meta = {}) => ({
  key,
  label,
  value: displayValue(value),
  ...meta
})

const summarizeObject = item => {
  if (!item || typeof item !== 'object' || Array.isArray(item)) {
    return ''
  }
  const primary = firstValue(
    item.code,
    item.name,
    item.alias,
    item.tableName,
    item.expression,
    item.normalizedExpression,
    item.sourceExpression,
    item.sourceColumn,
    item.mvColumn,
    item.leftColumn,
    item.rightColumn,
    item.sourceName,
    item.sourceKind,
    item.status
  )
  const secondary = [
    item.description,
    item.reason,
    item.status,
    item.joinType,
    item.targetExpression,
    item.rewriteExpression,
    item.sourceTable,
    item.targetTable,
    item.evidenceRef
  ]
    .filter(hasValue)
    .filter(value => String(value) !== String(primary || ''))
  const parts = [primary, ...secondary].filter(hasValue).map(value => String(value))
  return parts.join(' · ')
}

const buildSimpleRows = (values, keyPrefix) =>
  asArray(values).map((item, index) => {
    if (item && typeof item === 'object' && !Array.isArray(item)) {
      return {
        key: `${keyPrefix}-${index}`,
        name: displayValue(firstValue(item.name, item.column, item.alias, item.expression, item.code, item.tableName, item.sourceName)),
        detail: displayValue(summarizeObject(item)),
        raw: item
      }
    }
    return {
      key: `${keyPrefix}-${index}`,
      name: displayValue(item),
      detail: '-',
      raw: item
    }
  })

const buildMeasureRows = measures =>
  asArray(measures).map((item, index) => {
    if (item && typeof item === 'object' && !Array.isArray(item)) {
      return {
        key: `measure-${index}`,
        name: displayValue(firstValue(item.name, item.alias, item.outputName, item.rewriteAlias)),
        sourceExpression: displayValue(firstValue(item.sourceExpression, item.expression, item.sourceColumn)),
        rewriteExpression: displayValue(firstValue(item.rewriteExpression, item.targetExpression, item.mvExpression)),
        aggregateFunction: displayValue(firstValue(item.aggregateFunction, item.functionName, item.function)),
        mergeable: boolDisplay(item.mergeable)
      }
    }
    return {
      key: `measure-${index}`,
      name: displayValue(item),
      sourceExpression: '-',
      rewriteExpression: '-',
      aggregateFunction: '-',
      mergeable: '-'
    }
  })

const buildPredicateRows = (values, keyPrefix) =>
  asArray(values).map((item, index) => {
    if (item && typeof item === 'object' && !Array.isArray(item)) {
      return {
        key: `${keyPrefix}-${index}`,
        expression: displayValue(firstValue(item.expression, item.normalizedExpression, item.text, item.code)),
        context: displayValue(firstValue(item.logicalContext, item.groupId, item.scope, item.kind, item.type)),
        reason: displayValue(firstValue(item.reason, item.description, item.evidenceRef))
      }
    }
    return {
      key: `${keyPrefix}-${index}`,
      expression: displayValue(item),
      context: '-',
      reason: '-'
    }
  })

const buildReasonRows = (values, keyPrefix) =>
  asArray(values).map((item, index) => {
    if (item && typeof item === 'object' && !Array.isArray(item)) {
      return {
        key: `${keyPrefix}-${index}`,
        code: displayValue(firstValue(item.code, item.warningCode, item.reasonCode, item.type)),
        description: displayValue(firstValue(item.description, item.message, item.reason, item.summary)),
        evidenceRef: displayValue(item.evidenceRef)
      }
    }
    return {
      key: `${keyPrefix}-${index}`,
      code: displayValue(item),
      description: '-',
      evidenceRef: '-'
    }
  })

const buildCoverageRows = coverage =>
  COVERAGE_LABELS.map(([key, label]) => ({
    key,
    label,
    passed: coverage?.[key] === true,
    value: boolDisplay(coverage?.[key])
  }))

const buildJoinGraphRows = joinGraph =>
  asArray(joinGraph).map((item, index) => {
    if (item && typeof item === 'object' && !Array.isArray(item)) {
      return {
        key: `join-graph-${index}`,
        joinType: displayValue(firstValue(item.joinType, item.type)),
        left: displayValue(firstValue(item.leftTable, item.leftAlias, item.leftColumn, item.left)),
        right: displayValue(firstValue(item.rightTable, item.rightAlias, item.rightColumn, item.right)),
        condition: displayValue(firstValue(item.condition, item.expression, item.onExpression))
      }
    }
    return {
      key: `join-graph-${index}`,
      joinType: '-',
      left: displayValue(item),
      right: '-',
      condition: '-'
    }
  })

const buildEvidenceDetailRows = value => {
  if (Array.isArray(value)) {
    return value.map((item, index) => ({
      key: `item-${index}`,
      label: String(index + 1),
      value: displayValue(item)
    }))
  }
  if (value && typeof value === 'object') {
    return Object.entries(value).map(([key, entry]) => ({
      key,
      label: key,
      value: displayValue(entry)
    }))
  }
  return hasValue(value)
    ? [
        {
          key: 'value',
          label: 'value',
          value: displayValue(value)
        }
      ]
    : []
}

const buildEvidenceSections = artifact =>
  DETAIL_SECTION_KEYS.map(([key, title]) => ({
    key,
    title,
    rows: buildEvidenceDetailRows(artifact?.[key])
  })).filter(section => section.rows.length > 0)

const buildSqlBlocks = artifact =>
  SQL_BLOCKS.map(([key, label]) => field(key, label, artifact?.[key])).filter(item => item.value !== '-')

const buildPredicateGroups = artifact =>
  PREDICATE_GROUPS.map(([key, title]) => ({
    key,
    title,
    rows: buildPredicateRows(artifact?.[key], key)
  }))

export const mvTypeDetail = mvType =>
  MV_TYPE_DETAILS[String(mvType || '').toUpperCase()] || {
    title: '未知高级 MV 类型',
    description: '后端未返回受支持的高级 MV 类型，前端只展示原始字段，不做语义推断。'
  }

export const artifactStatusDetail = artifactStatus =>
  STATUS_DETAILS[String(artifactStatus || '').toUpperCase()] || {
    label: displayValue(artifactStatus),
    description: '后端未返回标准产物状态，前端只展示原始状态。'
  }

export const buildRuntimeRewriteSqlSourceNotice = selection => {
  if (!selection || !hasValue(selection.source)) {
    return ''
  }
  if (selection.source === 'ACCELERATION_ARTIFACT_REWRITE_SQL') {
    const artifact = selection.accelerationArtifact || {}
    return `recommendedSqlText 来源：来自 ${displayValue(artifact.mvName)} 的 accelerationArtifact.rewriteSql；创建改写记录后仍需审批、发布和 runtime binding ACTIVE 才能生效。`
  }
  return 'recommendedSqlText 来源：来自 recommendation.recommendedSqlText 或 diff.recommendedSql；创建改写记录后仍需审批、发布和 runtime binding ACTIVE 才能生效。'
}

export const buildAccelerationArtifactDisplay = artifact => {
  if (!artifact) {
    return {
      overviewRows: [],
      blockingRows: [],
      reviewWarningRows: [],
      grainRows: [],
      dimensionRows: [],
      measureRows: [],
      predicateGroups: buildPredicateGroups({}),
      coverageRows: buildCoverageRows({}),
      joinGraphRows: [],
      evidenceSections: [],
      sqlBlocks: [],
      statusDetail: artifactStatusDetail(''),
      mvTypeDetail: mvTypeDetail('')
    }
  }
  const mvTypeInfo = mvTypeDetail(artifact.mvType)
  const statusInfo = artifactStatusDetail(artifact.artifactStatus)
  return {
    overviewRows: [
      field('mvTypeZh', 'MV 类型说明', `${mvTypeInfo.title}：${mvTypeInfo.description}`),
      field('mvType', 'mvType', artifact.mvType),
      field('artifactStatus', '产物状态', artifact.artifactStatus),
      field('artifactStatusMeaning', '状态说明', statusInfo.description),
      field('mvName', '物化视图名', artifact.mvName),
      field('targetEngine', '目标引擎', artifact.targetEngine),
      field('targetDatasource', '目标数据源', artifact.targetDatasource),
      field('dialect', '方言', artifact.dialect),
      field('refreshStrategy', '刷新策略', artifact.refreshStrategy),
      field('runtimeRewriteBinding', 'runtime 绑定', artifact.runtimeRewriteBinding),
      field('governanceBoundary', '治理边界', artifact.governanceBoundary)
    ],
    blockingRows: buildReasonRows(artifact.blockingReasons, 'blocking'),
    reviewWarningRows: buildReasonRows(artifact.reviewWarnings, 'review-warning'),
    grainRows: buildSimpleRows(artifact.grain, 'grain'),
    dimensionRows: buildSimpleRows(artifact.dimensions, 'dimension'),
    measureRows: buildMeasureRows(artifact.measures),
    predicateGroups: buildPredicateGroups(artifact),
    coverageRows: buildCoverageRows(artifact.coverage || {}),
    joinGraphRows: buildJoinGraphRows(artifact.joinGraph),
    evidenceSections: buildEvidenceSections(artifact),
    sqlBlocks: buildSqlBlocks(artifact),
    statusDetail: statusInfo,
    mvTypeDetail: mvTypeInfo
  }
}
