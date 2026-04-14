<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue';

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '');

const messages = {
  zh: {
    app: {
      brand: 'SQLForge',
      title: '多引擎 SQL 交付控制台',
      description: '前后端分离，前端使用 Vue + JavaScript + CSS，后端使用 Java 8 + Spring Boot。界面按工作区拆分，不再使用单页长滚动。',
      sidebarNote: '按 Harness engineering 方式推进：先读仓库文档，再交付大模块，再记录检查点。',
      fallback: '当前展示的是本地回退数据，因为后端接口暂时不可用：{message}',
      locale: '语言',
      storage: '元数据持久化，密钥不落盘',
      architecture: '前后端分离',
      standards: 'UTF-8 / Unix(LF) / ARM'
    },
    menu: {
      dashboard: '总览',
      connections: '连接管理',
      analysis: '结构分析',
      workflows: '工作流',
      pressure: '压测编排',
      handoff: '交付门禁'
    },
    menuDesc: {
      dashboard: '查看技术栈、架构规则、引擎支持与运行摘要',
      connections: '登记连接、做离线校验、探测与 SQL 预览',
      analysis: '对单条或批量 SQL 做纯结构意图识别',
      workflows: '运行 BI 发布评估、容量规划与执行计划稳定性分析',
      pressure: '从日批 SQL 生成压测计划、蓝图、Manifest 与排期',
      handoff: '输出交付包、汇报稿与 readiness gate 结论'
    },
    sections: {
      delivery: '当前交付范围',
      stack: '技术栈快照',
      engines: '支持的数据引擎',
      overview: '连接模块总览',
      distribution: '按引擎分布',
      operations: '最近状态',
      rules: '架构约束',
      editor: '连接编辑器',
      savedConnections: '已保存连接',
      probe: '连通性探测',
      preview: 'SQL 预览',
      activity: '连接活动',
      analysisWorkbench: 'SQL 结构分析工作台',
      analysisResults: '结构分析结果',
      workflowWorkbench: 'Java 工作流工作台',
      workflowResults: '工作流产物',
      workflowBaselines: 'BI 基线历史',
      pressureWorkbench: '压测编排工作台',
      pressureResults: '压测编排产物',
      handoffWorkbench: '交付与门禁工作台',
      handoffResults: '交付与门禁产物',
      gate: 'Readiness Gate',
      rawJson: '原始 JSON',
      quickActions: '快捷入口'
    },
    labels: {
      frontend: '前端',
      backend: '后端',
      architecture: '架构',
      databases: '数据库 / 引擎',
      standards: '工程规范',
      name: '连接名称',
      engine: '引擎',
      host: '主机',
      port: '端口',
      catalog: 'Catalog / Database',
      username: '用户名',
      password: '密码',
      ssl: '启用 SSL',
      tenantId: '租户标识',
      slaMs: '目标 SLA(ms)',
      baselineQps: '基线 QPS',
      trafficGrowthFactor: '流量增长系数',
      avgServiceTimeSec: '平均服务时长(秒)',
      currentWorkers: '当前 Worker 数',
      targetP99Ms: '目标 P99(ms)',
      hotDataGb: '热数据量(GB)',
      metricsJson: '指标样本 JSON',
      queryMixJson: '查询混合 JSON',
      dataProfileJson: '数据画像 JSON',
      resourceBreakdownJson: '资源拆分 JSON',
      historicalPlanJson: '历史最优计划 JSON',
      currentPlanJson: '当前计划 JSON',
      candidatePlansJson: '候选计划 JSON',
      source: '样本来源',
      batchId: '批次标识',
      targetConcurrency: '目标并发',
      singleSql: '输入 SQL',
      batchSql: '批量 SQL 输入',
      maxRows: '最大返回行数',
      activeConnection: '当前连接',
      fingerprint: '指纹',
      statementType: '语句类型',
      tables: '涉及表',
      joinType: 'Join 类型',
      complexity: '复杂度',
      loadClass: '负载分级',
      parsedStatements: '已解析 SQL',
      intentTags: '意图标签数',
      splitMode: '拆分方式',
      status: '状态',
      lastUpdated: '最近更新',
      decision: '结论',
      blockers: '阻塞项',
      prerequisites: '前置条件',
      recommendations: '建议动作',
      noData: '暂无',
      stageCount: '阶段数',
      artifactCount: 'Artifact 数',
      agendaItems: 'Agenda 项数',
      totalDuration: '总时长',
      rows: '结果行数',
      driver: '驱动',
      api: 'API',
      health: '后端'
    },
    buttons: {
      zh: '中文',
      en: 'EN',
      newConnection: '新建连接',
      validate: '离线校验',
      create: '登记连接',
      update: '更新连接',
      delete: '删除',
      load: '载入',
      probe: '执行探测',
      preview: '执行预览',
      analyze: '结构分析',
      pressurePlan: '生成压测计划',
      blueprint: '生成场景蓝图',
      manifest: '生成执行清单',
      schedule: '生成阶段排期',
      runPackage: '生成交付包',
      briefing: '生成汇报稿',
      gate: '生成准入门禁',
      biRelease: '运行 BI 发布评估',
      refreshBaselines: '刷新基线历史',
      capacityPlan: '运行容量规划',
      planStability: '运行计划稳定性',
      openConnections: '去连接管理',
      openAnalysis: '去结构分析',
      openWorkflows: '去工作流',
      openPressure: '去压测编排',
      openHandoff: '去交付门禁'
    },
    mode: {
      single: '单条分析',
      batch: '日批量分析'
    },
    status: {
      checking: '检查中',
      healthy: '正常',
      offline: '离线',
      driverReady: '驱动就绪',
      driverMissing: '驱动缺失',
      reachable: '可达',
      unreachable: '不可达',
      ready: '可推进',
      caution: '谨慎推进',
      blocked: '阻塞',
      approved: '已批准',
      'needs-optimization': '需优化'
    },
    empty: {
      overview: '后端返回后会展示连接概览。',
      connections: '当前还没有保存的连接。',
      activity: '选择一个已保存连接后会显示活动记录。',
      probe: '完成探测后会在这里显示连通性、JDBC 与驱动层信息。',
      preview: '执行预览后会在这里展示返回数据。',
      analysis: '完成结构分析后会在这里展示摘要与逐条画像。',
      pressure: '生成任一压测产物后会在这里展示结果。',
      handoff: '生成交付包、汇报稿或 readiness gate 后会在这里展示结果。',
      workflows: '运行任一 Java 工作流后，会在这里展示决策、摘要和原始 JSON。',
      workflowBaselines: '已持久化的 BI 基线会在这里展示，便于跨重启对比。'
    },
    hints: {
      batchSplit: '建议使用分号或空行分隔多条 SQL。这里只做结构分析，不会连接数据库或执行 SQL。',
      workflows: '这些工作流已在 Java 后端实现，前端这里只负责参数装配、触发执行和结果展示。',
      pressure: '压测编排依赖同一批次的原始 SQL 文本，只做纯结构推演。',
      handoff: '交付层产物会沿用同一批次输入，生成适合评审与放行的结构化结论。',
      rules: [
        '业务行为优先进入 application/use-case 层，而不是直接堆到入口层。',
        '跨模块规则要写入文档、测试或工具，不放在一次性 prompt 里。',
        '完成较大的功能族后再做一次 Git 提交与 checkpoint 标记。'
      ]
    },
    messages: {
      unexpected: '后端返回了异常响应',
      connectionLoaded: '已加载连接：{name}。如需执行探测或预览，请重新输入密码。',
      switchedToCreate: '已切换到新建模式。',
      connectionCreated: '连接已登记：{name}',
      connectionUpdated: '连接已更新：{name}',
      connectionDeleted: '连接已删除：{id}',
      validationSuccess: '离线校验通过',
      probeSuccess: '探测完成',
      previewSuccess: '预览完成',
      analysisSingle: '结构分析已完成，结果仅基于 SQL 形态。',
      analysisBatch: '批量结构分析已完成，共解析 {count} 条 SQL。',
      pressure: '压测计划已生成，共编排 {count} 条 SQL。',
      blueprint: '场景蓝图已生成，共规划 {count} 个阶段。',
      manifest: '执行清单已生成，共 {count} 个阶段。',
      schedule: '阶段排期已生成，总时长 {minutes} 分钟。',
      runPackage: '交付包已生成，共 {count} 个主 artifact。',
      briefing: '汇报稿已生成，agenda 共 {count} 项。',
      gate: 'Readiness gate 已完成，结论为 {decision}。',
      invalidJson: 'JSON 输入格式不正确：{field}',
      biRelease: 'BI 发布评估已完成，结论为 {decision}。',
      workflowBaselines: '已加载 {count} 条 BI 基线历史。',
      capacityPlan: '容量规划已完成，新增 Worker 需求 {count}。',
      planStability: '计划稳定性分析已完成，动作建议为 {decision}。'
    }
  },
  en: {
    app: {
      brand: 'SQLForge',
      title: 'Multi-Engine SQL Delivery Console',
      description: 'Separated frontend and backend, built with Vue + JavaScript + CSS on the UI and Java 8 + Spring Boot on the backend. The interface is organized as workspaces instead of one long page.',
      sidebarNote: 'Run in a Harness engineering style: read repo docs first, deliver larger modules, then record checkpoints.',
      fallback: 'Local fallback data is being shown because backend APIs are currently unavailable: {message}',
      locale: 'Language',
      storage: 'metadata persisted, secrets excluded',
      architecture: 'frontend/backend separated',
      standards: 'UTF-8 / Unix(LF) / ARM'
    },
    menu: {
      dashboard: 'Overview',
      connections: 'Connections',
      analysis: 'Analysis',
      workflows: 'Workflows',
      pressure: 'Pressure',
      handoff: 'Handoff'
    },
    menuDesc: {
      dashboard: 'See the stack, architecture rules, engine support, and runtime summary',
      connections: 'Register connections, run offline validation, probes, and SQL previews',
      analysis: 'Run pure structural SQL intent analysis for single or daily batch input',
      workflows: 'Run BI release evaluation, capacity planning, and plan-stability workflows',
      pressure: 'Generate planning artifacts from daily SQL batches',
      handoff: 'Produce handoff packages, briefing reports, and readiness decisions'
    },
    sections: {
      delivery: 'Current Delivery Scope',
      stack: 'Stack Snapshot',
      engines: 'Supported Engines',
      overview: 'Connection Module Overview',
      distribution: 'Engine Distribution',
      operations: 'Recent Status',
      rules: 'Architecture Rules',
      editor: 'Connection Editor',
      savedConnections: 'Saved Connections',
      probe: 'Connectivity Probe',
      preview: 'SQL Preview',
      activity: 'Connection Activity',
      analysisWorkbench: 'SQL Structural Analysis Workspace',
      analysisResults: 'Structural Analysis Output',
      workflowWorkbench: 'Java Workflow Workspace',
      workflowResults: 'Workflow Output',
      workflowBaselines: 'BI Baseline History',
      pressureWorkbench: 'Pressure Planning Workspace',
      pressureResults: 'Pressure Planning Artifacts',
      handoffWorkbench: 'Handoff and Gate Workspace',
      handoffResults: 'Handoff Artifacts',
      gate: 'Readiness Gate',
      rawJson: 'Raw JSON',
      quickActions: 'Quick Actions'
    },
    labels: {
      frontend: 'Frontend',
      backend: 'Backend',
      architecture: 'Architecture',
      databases: 'Databases / Engines',
      standards: 'Engineering Rules',
      name: 'Connection Name',
      engine: 'Engine',
      host: 'Host',
      port: 'Port',
      catalog: 'Catalog / Database',
      username: 'Username',
      password: 'Password',
      ssl: 'SSL Enabled',
      tenantId: 'Tenant Id',
      slaMs: 'Target SLA(ms)',
      baselineQps: 'Baseline QPS',
      trafficGrowthFactor: 'Traffic Growth Factor',
      avgServiceTimeSec: 'Avg Service Time(sec)',
      currentWorkers: 'Current Workers',
      targetP99Ms: 'Target P99(ms)',
      hotDataGb: 'Hot Data(GB)',
      metricsJson: 'Metrics JSON',
      queryMixJson: 'Query Mix JSON',
      dataProfileJson: 'Data Profile JSON',
      resourceBreakdownJson: 'Resource Breakdown JSON',
      historicalPlanJson: 'Historical Best Plan JSON',
      currentPlanJson: 'Current Plan JSON',
      candidatePlansJson: 'Candidate Plans JSON',
      source: 'Sample Source',
      batchId: 'Batch Id',
      targetConcurrency: 'Target Concurrency',
      singleSql: 'Input SQL',
      batchSql: 'Batch SQL Input',
      maxRows: 'Max Rows',
      activeConnection: 'Active Connection',
      fingerprint: 'Fingerprint',
      statementType: 'Statement Type',
      tables: 'Tables',
      joinType: 'Join Type',
      complexity: 'Complexity',
      loadClass: 'Load Class',
      parsedStatements: 'Parsed SQL',
      intentTags: 'Intent Tag Count',
      splitMode: 'Split Mode',
      status: 'Status',
      lastUpdated: 'Last Updated',
      decision: 'Decision',
      blockers: 'Blockers',
      prerequisites: 'Prerequisites',
      recommendations: 'Recommendations',
      noData: 'n/a',
      stageCount: 'Stages',
      artifactCount: 'Artifacts',
      agendaItems: 'Agenda Items',
      totalDuration: 'Total Duration',
      rows: 'Rows',
      driver: 'Driver',
      api: 'API',
      health: 'Backend'
    },
    buttons: {
      zh: '中文',
      en: 'EN',
      newConnection: 'New Connection',
      validate: 'Offline Validate',
      create: 'Register Connection',
      update: 'Update Connection',
      delete: 'Delete',
      load: 'Load',
      probe: 'Run Probe',
      preview: 'Run Preview',
      analyze: 'Analyze Structure',
      pressurePlan: 'Build Pressure Plan',
      blueprint: 'Build Scenario Blueprint',
      manifest: 'Build Execution Manifest',
      schedule: 'Build Campaign Schedule',
      runPackage: 'Build Run Package',
      briefing: 'Build Briefing Report',
      gate: 'Build Readiness Gate',
      biRelease: 'Run BI Release',
      refreshBaselines: 'Refresh Baselines',
      capacityPlan: 'Run Capacity Plan',
      planStability: 'Run Plan Stability',
      openConnections: 'Open Connections',
      openAnalysis: 'Open Analysis',
      openWorkflows: 'Open Workflows',
      openPressure: 'Open Pressure',
      openHandoff: 'Open Handoff'
    },
    mode: {
      single: 'Single Statement',
      batch: 'Daily Batch'
    },
    status: {
      checking: 'checking',
      healthy: 'healthy',
      offline: 'offline',
      driverReady: 'driver ready',
      driverMissing: 'driver missing',
      reachable: 'reachable',
      unreachable: 'unreachable',
      ready: 'ready',
      caution: 'caution',
      blocked: 'blocked',
      approved: 'approved',
      'needs-optimization': 'needs optimization'
    },
    empty: {
      overview: 'Connection overview will appear after the backend responds.',
      connections: 'No saved connections yet.',
      activity: 'Select a saved connection to see its activity history.',
      probe: 'Probe output will appear here after a connectivity check runs.',
      preview: 'Preview output will appear here after a SQL preview runs.',
      analysis: 'Structural summary and statement cards will appear here after analysis.',
      pressure: 'Pressure-planning results will appear here after any artifact is generated.',
      handoff: 'Run package, briefing report, or readiness gate output will appear here after generation.',
      workflows: 'Workflow decisions, summaries, and raw JSON will appear here after execution.',
      workflowBaselines: 'Persisted BI baselines will appear here for cross-restart comparison.'
    },
    hints: {
      batchSplit: 'Separate multiple statements with semicolons or blank lines. This path only performs structural analysis and never executes SQL.',
      workflows: 'These workflows already exist in the Java backend. The frontend only assembles parameters, triggers execution, and presents results.',
      pressure: 'Pressure planning reuses the same raw SQL batch and stays fully structure-driven.',
      handoff: 'Handoff artifacts reuse the same batch input to produce review and release-facing outputs.',
      rules: [
        'Business behavior should enter through application or use-case layers, not accumulate in entry handlers.',
        'Cross-cutting rules should live in docs, tests, or tooling instead of one-off prompts.',
        'Commit and tag after delivering a larger feature family instead of every small slice.'
      ]
    },
    messages: {
      unexpected: 'backend returned an unexpected response',
      connectionLoaded: 'Connection loaded: {name}. Re-enter the password before running a probe or preview.',
      switchedToCreate: 'Switched to create mode.',
      connectionCreated: 'Connection registered: {name}',
      connectionUpdated: 'Connection updated: {name}',
      connectionDeleted: 'Connection deleted: {id}',
      validationSuccess: 'Offline validation passed',
      probeSuccess: 'Probe completed',
      previewSuccess: 'Preview completed',
      analysisSingle: 'Structural analysis completed with SQL-shape-only output.',
      analysisBatch: 'Batch structural analysis completed. Parsed {count} SQL statements.',
      pressure: 'Pressure plan generated for {count} SQL statements.',
      blueprint: 'Scenario blueprint generated with {count} stages.',
      manifest: 'Execution manifest generated with {count} stages.',
      schedule: 'Campaign schedule generated with a {minutes}-minute duration.',
      runPackage: 'Run package generated with {count} primary artifacts.',
      briefing: 'Briefing report generated with {count} agenda items.',
      gate: 'Readiness gate completed with a {decision} decision.',
      invalidJson: 'Invalid JSON input: {field}',
      biRelease: 'BI release evaluation completed with a {decision} decision.',
      workflowBaselines: 'Loaded {count} BI baseline records.',
      capacityPlan: 'Capacity planning completed with {count} additional workers required.',
      planStability: 'Plan stability analysis completed with a {decision} action.'
    }
  }
};

function resolveInitialLocale() {
  if (typeof window === 'undefined') {
    return 'zh';
  }

  const savedLocale = window.localStorage.getItem('sqlforge-locale');
  return savedLocale === 'en' ? 'en' : 'zh';
}

function lookupCopy(localeKey, path) {
  return path.split('.').reduce((current, segment) => (current == null ? current : current[segment]), messages[localeKey]);
}

function formatCopy(template, params) {
  return String(template).replace(/\{(\w+)\}/g, (_, key) => String(params[key] ?? ''));
}

const locale = ref(resolveInitialLocale());
const activeMenu = ref('dashboard');
const health = ref('checking');
const engines = ref([]);
const driverAudit = ref([]);
const overview = ref(null);
const connections = ref([]);
const connectionActivity = ref([]);
const errorMessage = ref('');
const saveMessage = ref('');
const validationMessage = ref('');
const probeMessage = ref('');
const probeResult = ref(null);
const previewMessage = ref('');
const previewResult = ref(null);
const sqlIntentMessage = ref('');
const sqlIntentResult = ref(null);
const sqlPressurePlanMessage = ref('');
const sqlPressurePlanResult = ref(null);
const sqlScenarioBlueprintMessage = ref('');
const sqlScenarioBlueprintResult = ref(null);
const sqlExecutionManifestMessage = ref('');
const sqlExecutionManifestResult = ref(null);
const sqlCampaignScheduleMessage = ref('');
const sqlCampaignScheduleResult = ref(null);
const sqlRunPackageMessage = ref('');
const sqlRunPackageResult = ref(null);
const sqlBriefingReportMessage = ref('');
const sqlBriefingReportResult = ref(null);
const sqlReadinessGateMessage = ref('');
const sqlReadinessGateResult = ref(null);
const biReleaseMessage = ref('');
const workflowBaselineHistoryMessage = ref('');
const capacityPlanMessage = ref('');
const planStabilityMessage = ref('');
const biReleaseResult = ref(null);
const workflowBaselineHistoryResult = ref(null);
const capacityPlanResult = ref(null);
const planStabilityResult = ref(null);
const sqlIntentMode = ref('single');
const isSubmitting = ref(false);
const isProbing = ref(false);
const isPreviewing = ref(false);
const isAnalyzingIntent = ref(false);
const isBuildingPressurePlan = ref(false);
const isBuildingScenarioBlueprint = ref(false);
const isBuildingExecutionManifest = ref(false);
const isBuildingCampaignSchedule = ref(false);
const isBuildingRunPackage = ref(false);
const isBuildingBriefingReport = ref(false);
const isBuildingReadinessGate = ref(false);
const isRunningBiRelease = ref(false);
const isLoadingWorkflowBaselines = ref(false);
const isRunningCapacityPlan = ref(false);
const isRunningPlanStability = ref(false);
const selectedConnectionId = ref('');
const previewSql = ref('select 1 as health_check');
const previewMaxRows = ref(20);
const sqlIntentSource = ref('manual-sample');
const sqlIntentBatchId = ref('daily-sql-batch');
const sqlPressureTargetConcurrency = ref(48);
const sqlIntentInput = ref(
  "with recent_orders as (select user_id, amount from lake.orders where ds >= '2026-04-01') "
    + "select user_id, sum(amount) from recent_orders group by 1 order by sum(amount) desc"
);
const sqlIntentBatchInput = ref(
  "select id, user_name from lake.users where id = 42 limit 1;\n\n"
    + "select o.user_id, sum(o.amount) from lake.orders o join lake.dim_users u on o.user_id = u.user_id "
    + "where o.ds between '2026-04-01' and '2026-04-14' group by 1;"
);
const biReleaseForm = ref({
  tenantId: 'tenant-a',
  sql: "select user_id, sum(amount) from lake.orders where ds >= current_date - interval '7' day group by 1",
  slaMs: 5000,
  targetConcurrency: 20,
  dataProfileJson: '{\n  "fullRows": 120000000,\n  "sampleRows": 2500000,\n  "skew": 1.35,\n  "hotDataGb": 420\n}',
  metricsJson: '[\n  { "run": 1, "latencyMs": 3820, "gcPauseMs": 110 },\n  { "run": 2, "latencyMs": 4010, "gcPauseMs": 96 }\n]',
  resourceBreakdownJson: '{\n  "cpu": 0.76,\n  "memory": 0.62,\n  "io": 0.41\n}',
  queryMixJson: '[\n  { "name": "daily-report", "cpuTimeSec": 14.2, "arrivalRate": 120, "memoryPeakGb": 5.5 },\n  { "name": "lookup", "cpuTimeSec": 1.8, "arrivalRate": 640, "memoryPeakGb": 0.6 }\n]'
});
const capacityPlanForm = ref({
  baselineQps: 120,
  trafficGrowthFactor: 2.4,
  avgServiceTimeSec: 0.135,
  currentWorkers: 24,
  targetP99Ms: 4000,
  targetConcurrency: 64,
  hotDataGb: 2400,
  queryMixJson: '[\n  { "name": "dashboard", "cpuTimeSec": 2.4, "arrivalRate": 220, "memoryPeakGb": 1.2 },\n  { "name": "heavy-report", "cpuTimeSec": 11.8, "arrivalRate": 48, "memoryPeakGb": 6.4 }\n]'
});
const planStabilityForm = ref({
  tenantId: 'tenant-a',
  sql: 'select o.user_id, sum(o.amount) from lake.orders o join lake.dim_users u on o.user_id = u.user_id group by 1',
  slaMs: 5000,
  targetConcurrency: 20,
  historicalBestPlanJson: '{\n  "planHash": "best-1",\n  "latencyMs": 1220,\n  "distribution": "broadcast",\n  "statsAgeHours": 6,\n  "joinOrder": ["orders", "dim_users"]\n}',
  currentPlanJson: '{\n  "planHash": "curr-1",\n  "latencyMs": 1700,\n  "distribution": "partitioned",\n  "statsAgeHours": 36,\n  "joinOrder": ["dim_users", "orders"]\n}',
  candidatePlansJson: '[\n  {\n    "planHash": "cand-1",\n    "latencyMs": 1400,\n    "distribution": "broadcast",\n    "statsAgeHours": 8,\n    "joinOrder": ["orders", "dim_users"]\n  }\n]'
});
const suspendEngineDefaults = ref(false);

function defaultConnectionForm() {
  return {
    name: 'Primary Trino',
    engineCode: 'trino',
    host: 'trino.sqlforge.local',
    port: 8443,
    catalog: 'lakehouse',
    username: 'analyst',
    password: 'changeit',
    sslEnabled: true
  };
}

const form = ref(defaultConnectionForm());

const fallbackEngines = [
  {
    code: 'mysql',
    name: 'MySQL',
    category: 'oltp',
    defaultPort: 3306,
    transport: 'tcp',
    jdbcScheme: 'mysql',
    driverClassName: 'com.mysql.cj.jdbc.Driver',
    profileNote: 'default profile for transactional MySQL instances'
  },
  {
    code: 'trino',
    name: 'Trino',
    category: 'query-engine',
    defaultPort: 8080,
    transport: 'http',
    jdbcScheme: 'trino',
    driverClassName: 'io.trino.jdbc.TrinoDriver',
    profileNote: 'coordinator endpoint; many secure clusters use 8443'
  },
  {
    code: 'presto',
    name: 'Presto',
    category: 'query-engine',
    defaultPort: 8080,
    transport: 'http',
    jdbcScheme: 'presto',
    driverClassName: 'com.facebook.presto.jdbc.PrestoDriver',
    profileNote: 'classic coordinator endpoint for Presto deployments'
  },
  {
    code: 'clickhouse',
    name: 'ClickHouse',
    category: 'olap',
    defaultPort: 8123,
    transport: 'http',
    jdbcScheme: 'clickhouse',
    driverClassName: 'com.clickhouse.jdbc.ClickHouseDriver',
    profileNote: 'http endpoint; native tcp deployments often use 9000'
  },
  {
    code: 'mrs-hetu',
    name: 'MRS-Hetu',
    category: 'query-engine',
    defaultPort: 28443,
    transport: 'http',
    jdbcScheme: 'presto',
    driverClassName: 'io.hetu.core.jdbc.HetuDriver',
    profileNote: 'hetu-compatible coordinator profile for MRS distributions'
  },
  {
    code: 'kyligence',
    name: 'Kyligence',
    category: 'cube-engine',
    defaultPort: 7070,
    transport: 'http',
    jdbcScheme: 'kylin',
    driverClassName: 'org.apache.kylin.jdbc.Driver',
    profileNote: 'kylin-compatible profile used by Kyligence gateways'
  }
];

function t(path, params = {}) {
  const localized = lookupCopy(locale.value, path);
  const fallback = lookupCopy('zh', path);
  const resolved = localized == null ? fallback : localized;
  return typeof resolved === 'string' ? formatCopy(resolved, params) : resolved;
}

const menuItems = computed(() => [
  { id: 'dashboard', label: t('menu.dashboard'), description: t('menuDesc.dashboard') },
  { id: 'connections', label: t('menu.connections'), description: t('menuDesc.connections') },
  { id: 'analysis', label: t('menu.analysis'), description: t('menuDesc.analysis') },
  { id: 'workflows', label: t('menu.workflows'), description: t('menuDesc.workflows') },
  { id: 'pressure', label: t('menu.pressure'), description: t('menuDesc.pressure') },
  { id: 'handoff', label: t('menu.handoff'), description: t('menuDesc.handoff') }
]);

const activeMenuMeta = computed(() => menuItems.value.find((item) => item.id === activeMenu.value) || menuItems.value[0]);
const supportedEngines = computed(() => (engines.value.length ? engines.value : fallbackEngines));
const stackItems = computed(() => [
  { label: t('labels.frontend'), value: 'Vue + JavaScript + CSS' },
  { label: t('labels.backend'), value: 'Java 8 + Spring Boot' },
  { label: t('labels.architecture'), value: 'Frontend / Backend Separated' },
  { label: t('labels.databases'), value: 'MySQL, Trino, Presto, ClickHouse, MRS-Hetu, Kyligence' },
  { label: t('labels.standards'), value: 'UTF-8, Unix(LF), ARM-ready' }
]);
const deliveryItems = computed(() => (
  locale.value === 'en'
    ? [
        'Vue + JS + CSS frontend',
        'Java 8 + Spring Boot backend',
        'MySQL / Trino / Presto / ClickHouse / MRS-Hetu / Kyligence',
        'UTF-8 + Unix/LF + ARM-ready delivery',
        'Harness-aligned plans, specs, checkpoints'
      ]
    : [
        'Vue + JS + CSS 前端',
        'Java 8 + Spring Boot 后端',
        '支持 MySQL / Trino / Presto / ClickHouse / MRS-Hetu / Kyligence',
        'UTF-8 + Unix/LF + ARM 架构兼容',
        '按 Harness 方式维护计划、规格与检查点'
      ]
));
const architectureRules = computed(() => t('hints.rules'));
const latestMessages = computed(() => [
  validationMessage.value,
  saveMessage.value,
  probeMessage.value,
  previewMessage.value,
  sqlIntentMessage.value,
  biReleaseMessage.value,
  workflowBaselineHistoryMessage.value,
  capacityPlanMessage.value,
  planStabilityMessage.value,
  sqlPressurePlanMessage.value,
  sqlScenarioBlueprintMessage.value,
  sqlExecutionManifestMessage.value,
  sqlCampaignScheduleMessage.value,
  sqlRunPackageMessage.value,
  sqlBriefingReportMessage.value,
  sqlReadinessGateMessage.value
].filter(Boolean));
const overviewCards = computed(() => {
  if (!overview.value) {
    return [];
  }

  return [
    {
      label: locale.value === 'en' ? 'saved connections' : '已保存连接',
      value: overview.value.totalConnections ?? 0
    },
    {
      label: locale.value === 'en' ? 'drivers ready' : '驱动就绪',
      value: overview.value.driverReadyCount ?? 0
    },
    {
      label: locale.value === 'en' ? 'healthy probe' : '探测成功',
      value: overview.value.lastProbeHealthyCount ?? 0
    },
    {
      label: locale.value === 'en' ? 'healthy preview' : '预览成功',
      value: overview.value.lastPreviewHealthyCount ?? 0
    }
  ];
});
const analysisSummaryCards = computed(() => {
  if (!sqlIntentResult.value || !sqlIntentResult.value.summary) {
    return [];
  }

  const summary = sqlIntentResult.value.summary;
  return [
    { label: t('labels.parsedStatements'), value: sqlIntentResult.value.parsedStatementCount ?? summary.statementCount ?? 0 },
    {
      label: t('labels.loadClass'),
      value: Object.keys(summary.byLoadClass || {}).join(', ') || t('labels.noData')
    },
    {
      label: t('labels.intentTags'),
      value: Object.keys(summary.byIntentTag || {}).length
    },
    {
      label: t('labels.splitMode'),
      value: sqlIntentResult.value.splitMode || t('labels.noData')
    }
  ];
});
const pressurePanels = computed(() => [
  {
    id: 'pressure-plan',
    title: t('buttons.pressurePlan'),
    message: sqlPressurePlanMessage.value,
    result: sqlPressurePlanResult.value,
    badge: sqlPressurePlanResult.value?.parsedStatementCount
      ? `${sqlPressurePlanResult.value.parsedStatementCount} ${t('labels.parsedStatements')}`
      : ''
  },
  {
    id: 'scenario-blueprint',
    title: t('buttons.blueprint'),
    message: sqlScenarioBlueprintMessage.value,
    result: sqlScenarioBlueprintResult.value,
    badge: sqlScenarioBlueprintResult.value?.stages?.length
      ? `${sqlScenarioBlueprintResult.value.stages.length} ${t('labels.stageCount')}`
      : ''
  },
  {
    id: 'execution-manifest',
    title: t('buttons.manifest'),
    message: sqlExecutionManifestMessage.value,
    result: sqlExecutionManifestResult.value,
    badge: sqlExecutionManifestResult.value?.stages?.length
      ? `${sqlExecutionManifestResult.value.stages.length} ${t('labels.stageCount')}`
      : ''
  },
  {
    id: 'campaign-schedule',
    title: t('buttons.schedule'),
    message: sqlCampaignScheduleMessage.value,
    result: sqlCampaignScheduleResult.value,
    badge: sqlCampaignScheduleResult.value?.campaignSummary?.totalDurationMinutes
      ? `${sqlCampaignScheduleResult.value.campaignSummary.totalDurationMinutes} ${locale.value === 'en' ? 'min' : '分钟'}`
      : ''
  }
].filter((item) => item.message || item.result));
const workflowPanels = computed(() => [
  {
    id: 'bi-release',
    title: t('buttons.biRelease'),
    message: biReleaseMessage.value,
    result: biReleaseResult.value,
    badge: biReleaseResult.value?.decision || ''
  },
  {
    id: 'capacity-plan',
    title: t('buttons.capacityPlan'),
    message: capacityPlanMessage.value,
    result: capacityPlanResult.value,
    badge: capacityPlanResult.value?.capacityPlan?.workerPlan?.additionalWorkers != null
      ? `${capacityPlanResult.value.capacityPlan.workerPlan.additionalWorkers} ${locale.value === 'en' ? 'addl workers' : '新增 Worker'}`
      : ''
  },
  {
    id: 'plan-stability',
    title: t('buttons.planStability'),
    message: planStabilityMessage.value,
    result: planStabilityResult.value,
    badge: planStabilityResult.value?.stabilityAnalysis?.decision || ''
  }
].filter((item) => item.result));
const workflowBaselineItems = computed(() => (
  Array.isArray(workflowBaselineHistoryResult.value?.baselines) ? workflowBaselineHistoryResult.value.baselines : []
));
const handoffPanels = computed(() => [
  {
    id: 'run-package',
    title: t('buttons.runPackage'),
    message: sqlRunPackageMessage.value,
    result: sqlRunPackageResult.value,
    badge: sqlRunPackageResult.value?.artifacts?.length
      ? `${sqlRunPackageResult.value.artifacts.length} ${t('labels.artifactCount')}`
      : ''
  },
  {
    id: 'briefing-report',
    title: t('buttons.briefing'),
    message: sqlBriefingReportMessage.value,
    result: sqlBriefingReportResult.value,
    badge: sqlBriefingReportResult.value?.reviewAgenda?.length
      ? `${sqlBriefingReportResult.value.reviewAgenda.length} ${t('labels.agendaItems')}`
      : ''
  },
  {
    id: 'readiness-gate',
    title: t('sections.gate'),
    message: sqlReadinessGateMessage.value,
    result: sqlReadinessGateResult.value,
    badge: sqlReadinessGateResult.value?.decision ? readinessDecisionLabel(sqlReadinessGateResult.value.decision) : ''
  }
].filter((item) => item.message || item.result));
const previewColumns = computed(() => {
  if (!previewResult.value) {
    return [];
  }

  if (Array.isArray(previewResult.value.columns) && previewResult.value.columns.length) {
    return previewResult.value.columns.map((column) => column.name || column.label || String(column));
  }

  if (Array.isArray(previewResult.value.rows) && previewResult.value.rows.length) {
    return Object.keys(previewResult.value.rows[0]);
  }

  return [];
});

watch(
  locale,
  (value) => {
    if (typeof window !== 'undefined') {
      window.localStorage.setItem('sqlforge-locale', value);
    }
  },
  { immediate: true }
);

watch(
  () => form.value.engineCode,
  (engineCode) => {
    if (suspendEngineDefaults.value) {
      return;
    }

    const selectedEngine = supportedEngines.value.find((engine) => engine.code === engineCode);
    if (selectedEngine) {
      form.value.port = selectedEngine.defaultPort;
    }
  }
);

function parseJsonText(text) {
  if (!text) {
    return {};
  }

  try {
    return JSON.parse(text);
  } catch (error) {
    return { message: text };
  }
}

async function requestJson(path, options = {}) {
  const response = await fetch(`${apiBaseUrl}${path}`, options);
  const payload = parseJsonText(await response.text());

  if (!response.ok) {
    throw new Error(payload.message || t('messages.unexpected'));
  }

  return payload;
}

async function setFormState(nextForm) {
  suspendEngineDefaults.value = true;
  form.value = nextForm;
  await nextTick();
  suspendEngineDefaults.value = false;
}

async function loadSystemState() {
  try {
    const [healthPayload, enginesPayload, driverAuditPayload, overviewPayload, connectionsPayload] = await Promise.all([
      requestJson('/api/v1/system/health'),
      requestJson('/api/v1/system/engines'),
      requestJson('/api/v1/system/driver-audit'),
      requestJson('/api/v1/system/connection-overview'),
      requestJson('/api/v1/connections')
    ]);

    health.value = healthPayload.status || 'healthy';
    engines.value = Array.isArray(enginesPayload.engines) && enginesPayload.engines.length
      ? enginesPayload.engines
      : fallbackEngines;
    driverAudit.value = Array.isArray(driverAuditPayload.drivers) ? driverAuditPayload.drivers : [];
    overview.value = overviewPayload.overview || null;
    connections.value = Array.isArray(connectionsPayload.connections) ? connectionsPayload.connections : [];
    errorMessage.value = '';
  } catch (error) {
    health.value = 'offline';
    engines.value = fallbackEngines;
    driverAudit.value = fallbackEngines.map((engine) => ({
      engineCode: engine.code,
      engineName: engine.name,
      driverClassName: engine.driverClassName,
      available: false
    }));
    overview.value = null;
    connections.value = [];
    errorMessage.value = error.message;
  }
}

async function loadConnection(connection) {
  selectedConnectionId.value = connection.id;
  await setFormState({
    name: connection.name,
    engineCode: connection.engineCode,
    host: connection.host,
    port: connection.port,
    catalog: connection.catalog,
    username: connection.username,
    password: '',
    sslEnabled: connection.sslEnabled
  });
  saveMessage.value = t('messages.connectionLoaded', { name: connection.name });
  await loadConnectionActivity(connection.id);
}

async function resetEditor() {
  selectedConnectionId.value = '';
  connectionActivity.value = [];
  await setFormState(defaultConnectionForm());
  saveMessage.value = t('messages.switchedToCreate');
}

async function loadConnectionActivity(connectionId) {
  try {
    const payload = await requestJson(`/api/v1/connections/${connectionId}/activity`);
    connectionActivity.value = Array.isArray(payload.activity) ? payload.activity : [];
  } catch (error) {
    connectionActivity.value = [];
  }
}

async function validateConnection() {
  validationMessage.value = '';

  try {
    const payload = await requestJson('/api/v1/connections/validate', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(form.value)
    });

    validationMessage.value = Array.isArray(payload.validation?.messages) && payload.validation.messages.length
      ? payload.validation.messages.join('；')
      : t('messages.validationSuccess');
  } catch (error) {
    validationMessage.value = error.message;
  }
}

async function createConnection() {
  saveMessage.value = '';
  isSubmitting.value = true;

  try {
    const isUpdate = Boolean(selectedConnectionId.value);
    const payload = await requestJson(
      isUpdate ? `/api/v1/connections/${selectedConnectionId.value}` : '/api/v1/connections',
      {
        method: isUpdate ? 'PUT' : 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(form.value)
      }
    );

    if (isUpdate) {
      saveMessage.value = t('messages.connectionUpdated', { name: payload.connection.name });
      await loadConnectionActivity(payload.connection.id);
    } else {
      selectedConnectionId.value = payload.connection.id;
      saveMessage.value = t('messages.connectionCreated', { name: payload.connection.name });
      await loadConnectionActivity(payload.connection.id);
    }

    await loadSystemState();
  } catch (error) {
    saveMessage.value = error.message;
  } finally {
    isSubmitting.value = false;
  }
}

async function deleteConnection(connectionId) {
  saveMessage.value = '';

  try {
    await requestJson(`/api/v1/connections/${connectionId}`, {
      method: 'DELETE'
    });

    if (selectedConnectionId.value === connectionId) {
      await resetEditor();
    }
    saveMessage.value = t('messages.connectionDeleted', { id: connectionId });
    await loadSystemState();
  } catch (error) {
    saveMessage.value = error.message;
  }
}

async function probeConnection() {
  probeMessage.value = '';
  probeResult.value = null;
  isProbing.value = true;

  try {
    const payload = await requestJson('/api/v1/connections/probe', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        ...form.value,
        connectionId: selectedConnectionId.value || undefined
      })
    });

    probeResult.value = payload.probe;
    probeMessage.value = Array.isArray(payload.probe?.messages) && payload.probe.messages.length
      ? payload.probe.messages.join('；')
      : t('messages.probeSuccess');

    if (payload.connection?.id) {
      await loadConnectionActivity(payload.connection.id);
      await loadSystemState();
    }
  } catch (error) {
    probeMessage.value = error.message;
  } finally {
    isProbing.value = false;
  }
}

async function previewQuery() {
  previewMessage.value = '';
  previewResult.value = null;
  isPreviewing.value = true;

  try {
    const payload = await requestJson('/api/v1/connections/query-preview', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        ...form.value,
        connectionId: selectedConnectionId.value || undefined,
        sql: previewSql.value,
        maxRows: previewMaxRows.value
      })
    });

    previewResult.value = payload.preview;
    previewMessage.value = Array.isArray(payload.preview?.messages) && payload.preview.messages.length
      ? payload.preview.messages.join('；')
      : t('messages.previewSuccess');

    if (payload.connection?.id) {
      await loadConnectionActivity(payload.connection.id);
      await loadSystemState();
    }
  } catch (error) {
    previewMessage.value = error.message;
  } finally {
    isPreviewing.value = false;
  }
}

function buildPressurePayload() {
  return {
    batchId: sqlIntentBatchId.value,
    source: sqlIntentSource.value,
    targetConcurrency: sqlPressureTargetConcurrency.value,
    rawSqlText: sqlIntentBatchInput.value
  };
}

async function analyzeSqlIntent() {
  sqlIntentMessage.value = '';
  sqlIntentResult.value = null;
  isAnalyzingIntent.value = true;

  try {
    const payload = await requestJson(
      sqlIntentMode.value === 'batch'
        ? '/api/v1/sql/intent-analysis/daily-batch'
        : '/api/v1/sql/intent-analysis',
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(
          sqlIntentMode.value === 'batch'
            ? {
                batchId: sqlIntentBatchId.value,
                source: sqlIntentSource.value,
                rawSqlText: sqlIntentBatchInput.value
              }
            : {
                statements: [
                  {
                    id: 'manual-analysis',
                    source: sqlIntentSource.value,
                    sql: sqlIntentInput.value
                  }
                ]
              }
        )
      }
    );

    sqlIntentResult.value = payload;
    sqlIntentMessage.value = sqlIntentMode.value === 'batch'
      ? t('messages.analysisBatch', { count: payload.parsedStatementCount ?? 0 })
      : t('messages.analysisSingle');
  } catch (error) {
    sqlIntentMessage.value = error.message;
  } finally {
    isAnalyzingIntent.value = false;
  }
}

async function buildSqlPressurePlan() {
  sqlPressurePlanMessage.value = '';
  sqlPressurePlanResult.value = null;
  isBuildingPressurePlan.value = true;

  try {
    const payload = await requestJson('/api/v1/sql/intent-analysis/pressure-plan', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(buildPressurePayload())
    });

    sqlPressurePlanResult.value = payload;
    sqlPressurePlanMessage.value = t('messages.pressure', {
      count: payload.parsedStatementCount ?? 0
    });
  } catch (error) {
    sqlPressurePlanMessage.value = error.message;
  } finally {
    isBuildingPressurePlan.value = false;
  }
}

async function buildSqlScenarioBlueprint() {
  sqlScenarioBlueprintMessage.value = '';
  sqlScenarioBlueprintResult.value = null;
  isBuildingScenarioBlueprint.value = true;

  try {
    const payload = await requestJson('/api/v1/sql/intent-analysis/scenario-blueprint', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(buildPressurePayload())
    });

    sqlScenarioBlueprintResult.value = payload;
    sqlScenarioBlueprintMessage.value = t('messages.blueprint', {
      count: payload.stages?.length ?? 0
    });
  } catch (error) {
    sqlScenarioBlueprintMessage.value = error.message;
  } finally {
    isBuildingScenarioBlueprint.value = false;
  }
}

async function buildSqlExecutionManifest() {
  sqlExecutionManifestMessage.value = '';
  sqlExecutionManifestResult.value = null;
  isBuildingExecutionManifest.value = true;

  try {
    const payload = await requestJson('/api/v1/sql/intent-analysis/execution-manifest', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(buildPressurePayload())
    });

    sqlExecutionManifestResult.value = payload;
    sqlExecutionManifestMessage.value = t('messages.manifest', {
      count: payload.stages?.length ?? 0
    });
  } catch (error) {
    sqlExecutionManifestMessage.value = error.message;
  } finally {
    isBuildingExecutionManifest.value = false;
  }
}

async function buildSqlCampaignSchedule() {
  sqlCampaignScheduleMessage.value = '';
  sqlCampaignScheduleResult.value = null;
  isBuildingCampaignSchedule.value = true;

  try {
    const payload = await requestJson('/api/v1/sql/intent-analysis/campaign-schedule', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(buildPressurePayload())
    });

    sqlCampaignScheduleResult.value = payload;
    sqlCampaignScheduleMessage.value = t('messages.schedule', {
      minutes: payload.campaignSummary?.totalDurationMinutes ?? 0
    });
  } catch (error) {
    sqlCampaignScheduleMessage.value = error.message;
  } finally {
    isBuildingCampaignSchedule.value = false;
  }
}

async function buildSqlRunPackage() {
  sqlRunPackageMessage.value = '';
  sqlRunPackageResult.value = null;
  isBuildingRunPackage.value = true;

  try {
    const payload = await requestJson('/api/v1/sql/intent-analysis/run-package', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(buildPressurePayload())
    });

    sqlRunPackageResult.value = payload;
    sqlRunPackageMessage.value = t('messages.runPackage', {
      count: payload.artifacts?.length ?? 0
    });
  } catch (error) {
    sqlRunPackageMessage.value = error.message;
  } finally {
    isBuildingRunPackage.value = false;
  }
}

async function buildSqlBriefingReport() {
  sqlBriefingReportMessage.value = '';
  sqlBriefingReportResult.value = null;
  isBuildingBriefingReport.value = true;

  try {
    const payload = await requestJson('/api/v1/sql/intent-analysis/briefing-report', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(buildPressurePayload())
    });

    sqlBriefingReportResult.value = payload;
    sqlBriefingReportMessage.value = t('messages.briefing', {
      count: payload.reviewAgenda?.length ?? 0
    });
  } catch (error) {
    sqlBriefingReportMessage.value = error.message;
  } finally {
    isBuildingBriefingReport.value = false;
  }
}

async function buildSqlReadinessGate() {
  sqlReadinessGateMessage.value = '';
  sqlReadinessGateResult.value = null;
  isBuildingReadinessGate.value = true;

  try {
    const payload = await requestJson('/api/v1/sql/intent-analysis/readiness-gate', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(buildPressurePayload())
    });

    sqlReadinessGateResult.value = payload;
    sqlReadinessGateMessage.value = t('messages.gate', {
      decision: readinessDecisionLabel(payload.decision)
    });
  } catch (error) {
    sqlReadinessGateMessage.value = error.message;
  } finally {
    isBuildingReadinessGate.value = false;
  }
}

function parseStructuredJson(text, fieldKey, fallbackValue) {
  if (!text || !String(text).trim()) {
    return fallbackValue;
  }

  try {
    return JSON.parse(text);
  } catch (error) {
    throw new Error(t('messages.invalidJson', { field: t(fieldKey) }));
  }
}

async function runBiReleaseWorkflow() {
  biReleaseMessage.value = '';
  biReleaseResult.value = null;
  isRunningBiRelease.value = true;

  try {
    const payload = await requestJson('/api/v1/workflows/bi-release', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        tenantId: biReleaseForm.value.tenantId,
        sql: biReleaseForm.value.sql,
        slaMs: biReleaseForm.value.slaMs,
        targetConcurrency: biReleaseForm.value.targetConcurrency,
        dataProfile: parseStructuredJson(biReleaseForm.value.dataProfileJson, 'labels.dataProfileJson', null),
        metrics: parseStructuredJson(biReleaseForm.value.metricsJson, 'labels.metricsJson', []),
        resourceBreakdown: parseStructuredJson(
          biReleaseForm.value.resourceBreakdownJson,
          'labels.resourceBreakdownJson',
          null
        ),
        queryMix: parseStructuredJson(biReleaseForm.value.queryMixJson, 'labels.queryMixJson', [])
      })
    });

    biReleaseResult.value = payload;
    biReleaseMessage.value = t('messages.biRelease', {
      decision: payload.decision || t('labels.noData')
    });
    await loadWorkflowBaselineHistory();
  } catch (error) {
    biReleaseMessage.value = error.message;
  } finally {
    isRunningBiRelease.value = false;
  }
}

async function loadWorkflowBaselineHistory() {
  workflowBaselineHistoryMessage.value = '';
  isLoadingWorkflowBaselines.value = true;

  try {
    const payload = await requestJson('/api/v1/workflows/bi-release/baselines?limit=12');
    workflowBaselineHistoryResult.value = payload;
    workflowBaselineHistoryMessage.value = t('messages.workflowBaselines', {
      count: payload.baselineCount ?? payload.baselines?.length ?? 0
    });
  } catch (error) {
    workflowBaselineHistoryResult.value = null;
    workflowBaselineHistoryMessage.value = error.message;
  } finally {
    isLoadingWorkflowBaselines.value = false;
  }
}

async function runCapacityPlanWorkflow() {
  capacityPlanMessage.value = '';
  capacityPlanResult.value = null;
  isRunningCapacityPlan.value = true;

  try {
    const payload = await requestJson('/api/v1/workflows/capacity-plan', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        baselineQps: capacityPlanForm.value.baselineQps,
        trafficGrowthFactor: capacityPlanForm.value.trafficGrowthFactor,
        avgServiceTimeSec: capacityPlanForm.value.avgServiceTimeSec,
        currentWorkers: capacityPlanForm.value.currentWorkers,
        targetP99Ms: capacityPlanForm.value.targetP99Ms,
        targetConcurrency: capacityPlanForm.value.targetConcurrency,
        hotDataGb: capacityPlanForm.value.hotDataGb,
        queryMix: parseStructuredJson(capacityPlanForm.value.queryMixJson, 'labels.queryMixJson', [])
      })
    });

    capacityPlanResult.value = payload;
    capacityPlanMessage.value = t('messages.capacityPlan', {
      count: payload.capacityPlan?.workerPlan?.additionalWorkers ?? 0
    });
  } catch (error) {
    capacityPlanMessage.value = error.message;
  } finally {
    isRunningCapacityPlan.value = false;
  }
}

async function runPlanStabilityWorkflow() {
  planStabilityMessage.value = '';
  planStabilityResult.value = null;
  isRunningPlanStability.value = true;

  try {
    const payload = await requestJson('/api/v1/workflows/plan-stability', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        tenantId: planStabilityForm.value.tenantId,
        sql: planStabilityForm.value.sql,
        slaMs: planStabilityForm.value.slaMs,
        targetConcurrency: planStabilityForm.value.targetConcurrency,
        historicalBestPlan: parseStructuredJson(
          planStabilityForm.value.historicalBestPlanJson,
          'labels.historicalPlanJson',
          null
        ),
        currentPlan: parseStructuredJson(planStabilityForm.value.currentPlanJson, 'labels.currentPlanJson', null),
        candidatePlans: parseStructuredJson(planStabilityForm.value.candidatePlansJson, 'labels.candidatePlansJson', [])
      })
    });

    planStabilityResult.value = payload;
    planStabilityMessage.value = t('messages.planStability', {
      decision: payload.stabilityAnalysis?.decision || t('labels.noData')
    });
  } catch (error) {
    planStabilityMessage.value = error.message;
  } finally {
    isRunningPlanStability.value = false;
  }
}

function healthLabel(value) {
  return t(`status.${value}`) || value;
}

function driverStatusLabel(item) {
  return item.available ? t('status.driverReady') : t('status.driverMissing');
}

function readinessDecisionLabel(decision) {
  if (!decision) {
    return t('labels.noData');
  }

  const localized = t(`status.${decision}`);
  return localized || decision;
}

function workflowDecisionLabel(decision) {
  if (!decision) {
    return t('labels.noData');
  }

  const localized = t(`status.${decision}`);
  return localized || decision;
}

function decisionClass(decision) {
  if (decision === 'blocked') {
    return 'decision-blocked';
  }

  if (decision === 'approved' || decision === 'ready') {
    return 'decision-ready';
  }

  return 'decision-caution';
}

function probeHealthy(result) {
  return Boolean(result?.reachable ?? result?.healthy ?? result?.socketReachable ?? result?.driverConnected);
}

function formatJson(value) {
  return value ? JSON.stringify(value, null, 2) : '';
}

function previewCell(row, column) {
  const value = row?.[column];
  return value == null ? '' : typeof value === 'object' ? JSON.stringify(value) : String(value);
}

function statementTags(item) {
  return Array.isArray(item?.intentTags) ? item.intentTags : [];
}

function statementSignals(item) {
  return Array.isArray(item?.structure?.structuralSignals)
    ? item.structure.structuralSignals
    : Array.isArray(item?.structuralSignals)
      ? item.structuralSignals
      : [];
}

function statementAlerts(item) {
  return Array.isArray(item?.alerts)
    ? item.alerts
    : Array.isArray(item?.structuralAlerts)
      ? item.structuralAlerts
      : [];
}

onMounted(() => {
  loadSystemState();
  loadWorkflowBaselineHistory();
});
</script>

<template>
  <main class="workspace-shell">
    <aside class="workspace-sidebar">
      <section class="brand-card">
        <p class="eyebrow">{{ t('app.brand') }}</p>
        <h1>{{ t('app.title') }}</h1>
        <p class="sidebar-copy">{{ t('app.description') }}</p>
        <div class="status-stack">
          <span class="status-pill">{{ t('labels.health') }}: {{ healthLabel(health) }}</span>
          <span class="status-pill">{{ t('labels.api') }}: {{ apiBaseUrl }}</span>
        </div>
      </section>

      <section class="sidebar-card">
        <div class="sidebar-card-head">
          <strong>{{ t('app.locale') }}</strong>
        </div>
        <div class="locale-switch">
          <button
            class="locale-button"
            :class="{ active: locale === 'zh' }"
            type="button"
            @click="locale = 'zh'"
          >
            {{ t('buttons.zh') }}
          </button>
          <button
            class="locale-button"
            :class="{ active: locale === 'en' }"
            type="button"
            @click="locale = 'en'"
          >
            {{ t('buttons.en') }}
          </button>
        </div>
      </section>

      <nav class="sidebar-card menu-list" aria-label="workspace navigation">
        <button
          v-for="item in menuItems"
          :key="item.id"
          class="menu-button"
          :class="{ active: activeMenu === item.id }"
          type="button"
          @click="activeMenu = item.id"
        >
          <strong>{{ item.label }}</strong>
          <span>{{ item.description }}</span>
        </button>
      </nav>

      <section class="sidebar-card">
        <div class="sidebar-card-head">
          <strong>{{ t('sections.operations') }}</strong>
        </div>
        <p class="sidebar-note">{{ t('app.sidebarNote') }}</p>
        <div v-if="latestMessages.length" class="message-list compact">
          <p v-for="message in latestMessages.slice(0, 4)" :key="message" class="status-message">
            {{ message }}
          </p>
        </div>
      </section>
    </aside>

    <section class="workspace-main">
      <header class="workspace-header panel">
        <div>
          <p class="eyebrow">{{ activeMenuMeta.label }}</p>
          <h2>{{ activeMenuMeta.description }}</h2>
        </div>
        <div class="header-badges">
          <span class="badge">{{ t('app.architecture') }}</span>
          <span class="badge">{{ t('app.storage') }}</span>
          <span class="badge">{{ t('app.standards') }}</span>
        </div>
      </header>

      <section v-if="activeMenu === 'dashboard'" class="panel-grid">
        <article class="panel hero-panel span-2">
          <div class="panel-head">
            <div>
              <p class="eyebrow">{{ t('sections.delivery') }}</p>
              <h3>{{ t('app.title') }}</h3>
            </div>
            <div class="header-badges">
              <span class="badge">{{ t('labels.health') }}: {{ healthLabel(health) }}</span>
              <span class="badge">{{ t('labels.databases') }}</span>
            </div>
          </div>
          <p class="lead">{{ t('app.description') }}</p>
          <p v-if="errorMessage" class="warning-text">
            {{ t('app.fallback', { message: errorMessage }) }}
          </p>
          <div class="chip-row">
            <span v-for="item in deliveryItems" :key="item" class="tag-chip">{{ item }}</span>
          </div>
        </article>

        <article class="panel">
          <div class="panel-head">
            <div>
              <p class="eyebrow">{{ t('sections.stack') }}</p>
              <h3>{{ t('sections.stack') }}</h3>
            </div>
          </div>
          <div class="metric-grid">
            <div v-for="item in stackItems" :key="item.label" class="metric-card">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </article>

        <article class="panel">
          <div class="panel-head">
            <div>
              <p class="eyebrow">{{ t('sections.quickActions') }}</p>
              <h3>{{ t('sections.quickActions') }}</h3>
            </div>
          </div>
          <div class="action-row">
            <button class="ghost-button" type="button" @click="activeMenu = 'connections'">
              {{ t('buttons.openConnections') }}
            </button>
            <button class="ghost-button" type="button" @click="activeMenu = 'analysis'">
              {{ t('buttons.openAnalysis') }}
            </button>
            <button class="ghost-button" type="button" @click="activeMenu = 'workflows'">
              {{ t('buttons.openWorkflows') }}
            </button>
            <button class="ghost-button" type="button" @click="activeMenu = 'pressure'">
              {{ t('buttons.openPressure') }}
            </button>
            <button class="ghost-button" type="button" @click="activeMenu = 'handoff'">
              {{ t('buttons.openHandoff') }}
            </button>
          </div>
        </article>

        <article class="panel span-2">
          <div class="panel-head">
            <div>
              <p class="eyebrow">{{ t('sections.engines') }}</p>
              <h3>{{ t('sections.engines') }}</h3>
            </div>
          </div>
          <div class="engine-grid">
            <div v-for="engine in supportedEngines" :key="engine.code" class="engine-card">
              <strong>{{ engine.name }}</strong>
              <span>{{ engine.category }} · {{ engine.transport }} · {{ engine.defaultPort }}</span>
              <small>{{ engine.profileNote }}</small>
            </div>
          </div>
          <div class="audit-grid">
            <div v-for="item in driverAudit" :key="item.engineCode" class="audit-item">
              <div>
                <strong>{{ item.engineName }}</strong>
                <span>{{ item.driverClassName }}</span>
              </div>
              <em>{{ driverStatusLabel(item) }}</em>
            </div>
          </div>
        </article>

        <article class="panel">
          <div class="panel-head">
            <div>
              <p class="eyebrow">{{ t('sections.overview') }}</p>
              <h3>{{ t('sections.overview') }}</h3>
            </div>
          </div>
          <div v-if="overviewCards.length" class="metric-grid">
            <div v-for="item in overviewCards" :key="item.label" class="metric-card">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
          <p v-else class="empty-state">{{ t('empty.overview') }}</p>
        </article>

        <article class="panel">
          <div class="panel-head">
            <div>
              <p class="eyebrow">{{ t('sections.distribution') }}</p>
              <h3>{{ t('sections.distribution') }}</h3>
            </div>
          </div>
          <div v-if="overview?.engines?.length" class="audit-grid">
            <div v-for="item in overview.engines" :key="item.engineCode" class="audit-item">
              <div>
                <strong>{{ item.engineCode }}</strong>
                <span>{{ t('labels.status') }}</span>
              </div>
              <em>{{ item.connectionCount }} {{ locale === 'en' ? 'connections' : '个连接' }}</em>
            </div>
          </div>
          <p v-else class="empty-state">{{ t('empty.connections') }}</p>
        </article>

        <article class="panel">
          <div class="panel-head">
            <div>
              <p class="eyebrow">{{ t('sections.operations') }}</p>
              <h3>{{ t('sections.operations') }}</h3>
            </div>
          </div>
          <div v-if="latestMessages.length" class="message-list">
            <p v-for="message in latestMessages" :key="message" class="status-message">
              {{ message }}
            </p>
          </div>
          <p v-else class="empty-state">{{ t('empty.analysis') }}</p>
        </article>

        <article class="panel">
          <div class="panel-head">
            <div>
              <p class="eyebrow">{{ t('sections.rules') }}</p>
              <h3>{{ t('sections.rules') }}</h3>
            </div>
          </div>
          <ul class="bullet-list">
            <li v-for="rule in architectureRules" :key="rule">{{ rule }}</li>
          </ul>
        </article>
      </section>

      <section v-else-if="activeMenu === 'connections'" class="panel-grid">
        <article class="panel">
          <div class="panel-head">
            <div>
              <p class="eyebrow">{{ t('sections.editor') }}</p>
              <h3>{{ t('sections.editor') }}</h3>
            </div>
            <span class="badge">{{ selectedConnectionId || t('buttons.newConnection') }}</span>
          </div>

          <div class="form-grid">
            <label>
              <span>{{ t('labels.name') }}</span>
              <input v-model="form.name" type="text" />
            </label>
            <label>
              <span>{{ t('labels.engine') }}</span>
              <select v-model="form.engineCode">
                <option v-for="engine in supportedEngines" :key="engine.code" :value="engine.code">
                  {{ engine.name }}
                </option>
              </select>
            </label>
            <label>
              <span>{{ t('labels.host') }}</span>
              <input v-model="form.host" type="text" />
            </label>
            <label>
              <span>{{ t('labels.port') }}</span>
              <input v-model.number="form.port" type="number" min="1" />
            </label>
            <label>
              <span>{{ t('labels.catalog') }}</span>
              <input v-model="form.catalog" type="text" />
            </label>
            <label>
              <span>{{ t('labels.username') }}</span>
              <input v-model="form.username" type="text" />
            </label>
            <label class="span-2">
              <span>{{ t('labels.password') }}</span>
              <input v-model="form.password" type="password" />
            </label>
          </div>

          <label class="switch-row">
            <input v-model="form.sslEnabled" type="checkbox" />
            <span>{{ t('labels.ssl') }}</span>
          </label>

          <div class="action-row">
            <button class="primary-button" type="button" :disabled="isSubmitting" @click="createConnection">
              {{ selectedConnectionId ? t('buttons.update') : t('buttons.create') }}
            </button>
            <button class="ghost-button" type="button" @click="validateConnection">
              {{ t('buttons.validate') }}
            </button>
            <button class="ghost-button" type="button" @click="resetEditor">
              {{ t('buttons.newConnection') }}
            </button>
          </div>

          <p v-if="validationMessage" class="status-message success-text">{{ validationMessage }}</p>
          <p v-if="saveMessage" class="status-message">{{ saveMessage }}</p>
        </article>

        <article class="panel">
          <div class="panel-head">
            <div>
              <p class="eyebrow">{{ t('sections.savedConnections') }}</p>
              <h3>{{ t('sections.savedConnections') }}</h3>
            </div>
            <span class="badge">{{ connections.length }}</span>
          </div>
          <div v-if="connections.length" class="connection-list">
            <article v-for="connection in connections" :key="connection.id" class="connection-item">
              <div class="connection-copy">
                <strong>{{ connection.name }}</strong>
                <span>{{ connection.engineCode }} · {{ connection.host }}:{{ connection.port }}</span>
                <small>{{ connection.catalog }} · {{ connection.username }}</small>
              </div>
              <div class="connection-actions">
                <button class="ghost-button" type="button" @click="loadConnection(connection)">
                  {{ t('buttons.load') }}
                </button>
                <button class="danger-button" type="button" @click="deleteConnection(connection.id)">
                  {{ t('buttons.delete') }}
                </button>
              </div>
            </article>
          </div>
          <p v-else class="empty-state">{{ t('empty.connections') }}</p>
        </article>

        <article class="panel">
          <div class="panel-head">
            <div>
              <p class="eyebrow">{{ t('sections.probe') }}</p>
              <h3>{{ t('sections.probe') }}</h3>
            </div>
          </div>
          <div class="action-row">
            <button class="primary-button" type="button" :disabled="isProbing" @click="probeConnection">
              {{ t('buttons.probe') }}
            </button>
          </div>
          <p v-if="probeMessage" class="status-message">{{ probeMessage }}</p>

          <div v-if="probeResult" class="stack">
            <div class="metric-grid">
              <div class="metric-card">
                <span>{{ t('labels.status') }}</span>
                <strong>{{ probeHealthy(probeResult) ? t('status.reachable') : t('status.unreachable') }}</strong>
              </div>
              <div class="metric-card">
                <span>{{ t('labels.driver') }}</span>
                <strong>{{ probeResult.driverClassName || t('labels.noData') }}</strong>
              </div>
            </div>
            <pre class="json-block">{{ formatJson(probeResult) }}</pre>
          </div>
          <p v-else class="empty-state">{{ t('empty.probe') }}</p>
        </article>

        <article class="panel">
          <div class="panel-head">
            <div>
              <p class="eyebrow">{{ t('sections.preview') }}</p>
              <h3>{{ t('sections.preview') }}</h3>
            </div>
          </div>
          <label class="field-block">
            <span>{{ t('labels.singleSql') }}</span>
            <textarea v-model="previewSql" rows="8"></textarea>
          </label>
          <label class="field-inline compact">
            <span>{{ t('labels.maxRows') }}</span>
            <input v-model.number="previewMaxRows" type="number" min="1" max="1000" />
          </label>
          <div class="action-row">
            <button class="primary-button" type="button" :disabled="isPreviewing" @click="previewQuery">
              {{ t('buttons.preview') }}
            </button>
          </div>
          <p v-if="previewMessage" class="status-message">{{ previewMessage }}</p>

          <div v-if="previewResult" class="stack">
            <div class="metric-grid">
              <div class="metric-card">
                <span>{{ t('labels.rows') }}</span>
                <strong>{{ previewResult.rows?.length ?? 0 }}</strong>
              </div>
            </div>
            <div v-if="previewColumns.length && previewResult.rows?.length" class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th v-for="column in previewColumns" :key="column">{{ column }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, index) in previewResult.rows" :key="index">
                    <td v-for="column in previewColumns" :key="column">
                      {{ previewCell(row, column) }}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <pre class="json-block">{{ formatJson(previewResult) }}</pre>
          </div>
          <p v-else class="empty-state">{{ t('empty.preview') }}</p>
        </article>

        <article class="panel span-2">
          <div class="panel-head">
            <div>
              <p class="eyebrow">{{ t('sections.activity') }}</p>
              <h3>{{ t('sections.activity') }}</h3>
            </div>
          </div>
          <div v-if="connectionActivity.length" class="activity-list">
            <div v-for="item in connectionActivity" :key="`${item.timestamp}-${item.type}`" class="activity-item">
              <strong>{{ item.type || t('labels.noData') }}</strong>
              <span>{{ item.message || t('labels.noData') }}</span>
              <small>{{ item.timestamp || t('labels.noData') }}</small>
            </div>
          </div>
          <p v-else class="empty-state">{{ t('empty.activity') }}</p>
        </article>
      </section>

      <section v-else-if="activeMenu === 'analysis'" class="panel-grid">
        <article class="panel span-2">
          <div class="panel-head">
            <div>
              <p class="eyebrow">{{ t('sections.analysisWorkbench') }}</p>
              <h3>{{ t('sections.analysisWorkbench') }}</h3>
            </div>
          </div>
          <p class="section-note">{{ t('hints.batchSplit') }}</p>

          <div class="mode-switch">
            <button
              class="ghost-button"
              :class="{ active: sqlIntentMode === 'single' }"
              type="button"
              @click="sqlIntentMode = 'single'"
            >
              {{ t('mode.single') }}
            </button>
            <button
              class="ghost-button"
              :class="{ active: sqlIntentMode === 'batch' }"
              type="button"
              @click="sqlIntentMode = 'batch'"
            >
              {{ t('mode.batch') }}
            </button>
          </div>

          <div class="form-grid">
            <label>
              <span>{{ t('labels.source') }}</span>
              <input v-model="sqlIntentSource" type="text" />
            </label>
            <label v-if="sqlIntentMode === 'batch'">
              <span>{{ t('labels.batchId') }}</span>
              <input v-model="sqlIntentBatchId" type="text" />
            </label>
            <label v-if="sqlIntentMode === 'batch'">
              <span>{{ t('labels.targetConcurrency') }}</span>
              <input v-model.number="sqlPressureTargetConcurrency" type="number" min="1" max="500" />
            </label>
          </div>

          <template v-if="sqlIntentMode === 'batch'">
            <label class="field-block">
              <span>{{ t('labels.batchSql') }}</span>
              <textarea v-model="sqlIntentBatchInput" rows="12"></textarea>
            </label>
          </template>
          <template v-else>
            <label class="field-block">
              <span>{{ t('labels.singleSql') }}</span>
              <textarea v-model="sqlIntentInput" rows="10"></textarea>
            </label>
          </template>

          <div class="action-row">
            <button class="primary-button" type="button" :disabled="isAnalyzingIntent" @click="analyzeSqlIntent">
              {{ t('buttons.analyze') }}
            </button>
          </div>
          <p v-if="sqlIntentMessage" class="status-message">{{ sqlIntentMessage }}</p>
        </article>

        <article class="panel span-2">
          <div class="panel-head">
            <div>
              <p class="eyebrow">{{ t('sections.analysisResults') }}</p>
              <h3>{{ t('sections.analysisResults') }}</h3>
            </div>
          </div>

          <template v-if="sqlIntentResult">
            <div class="metric-grid">
              <div v-for="item in analysisSummaryCards" :key="item.label" class="metric-card">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </div>
            </div>

            <div class="statement-list">
              <article v-for="item in sqlIntentResult.analyses || []" :key="item.statementId" class="statement-card">
                <div class="panel-head">
                  <div>
                    <strong>{{ item.statementId }}</strong>
                    <p class="card-subtitle">
                      {{ item.structure?.statementType || t('labels.noData') }} ·
                      {{ item.pressureProfile?.loadClass || t('labels.noData') }}
                    </p>
                  </div>
                  <span class="badge">{{ item.pressureProfile?.complexityTier || t('labels.noData') }}</span>
                </div>

                <div class="definition-grid">
                  <div class="metric-card">
                    <span>{{ t('labels.fingerprint') }}</span>
                    <strong>{{ item.fingerprint || t('labels.noData') }}</strong>
                  </div>
                  <div class="metric-card">
                    <span>{{ t('labels.tables') }}</span>
                    <strong>{{ item.structure?.tables?.join(', ') || t('labels.noData') }}</strong>
                  </div>
                  <div class="metric-card">
                    <span>{{ t('labels.joinType') }}</span>
                    <strong>{{ item.structure?.joinType || t('labels.noData') }}</strong>
                  </div>
                  <div class="metric-card">
                    <span>{{ t('labels.complexity') }}</span>
                    <strong>
                      {{ item.pressureProfile?.complexityTier || t('labels.noData') }}
                      <small v-if="item.pressureProfile?.complexityScore != null">
                        ({{ item.pressureProfile.complexityScore }})
                      </small>
                    </strong>
                  </div>
                </div>

                <div v-if="statementTags(item).length" class="chip-row">
                  <span v-for="tag in statementTags(item)" :key="tag" class="tag-chip">{{ tag }}</span>
                </div>
                <div v-if="statementSignals(item).length" class="chip-row">
                  <span v-for="signal in statementSignals(item)" :key="signal" class="signal-chip">{{ signal }}</span>
                </div>
                <ul v-if="statementAlerts(item).length" class="bullet-list">
                  <li v-for="alert in statementAlerts(item)" :key="`${item.statementId}-${alert.code}-${alert.message}`">
                    {{ alert.code || 'alert' }} · {{ alert.message || alert }}
                  </li>
                </ul>
              </article>
            </div>

            <details class="json-details">
              <summary>{{ t('sections.rawJson') }}</summary>
              <pre class="json-block">{{ formatJson(sqlIntentResult) }}</pre>
            </details>
          </template>

          <p v-else class="empty-state">{{ t('empty.analysis') }}</p>
        </article>
      </section>

      <section v-else-if="activeMenu === 'workflows'" class="panel-grid">
        <article class="panel span-2">
          <div class="panel-head">
            <div>
              <p class="eyebrow">{{ t('sections.workflowWorkbench') }}</p>
              <h3>{{ t('sections.workflowWorkbench') }}</h3>
            </div>
          </div>
          <p class="section-note">{{ t('hints.workflows') }}</p>
        </article>

        <article class="panel">
          <div class="panel-head">
            <div>
              <strong>{{ t('buttons.biRelease') }}</strong>
              <p class="card-subtitle">`POST /api/v1/workflows/bi-release`</p>
            </div>
          </div>

          <div class="form-grid">
            <label>
              <span>{{ t('labels.tenantId') }}</span>
              <input v-model="biReleaseForm.tenantId" type="text" />
            </label>
            <label>
              <span>{{ t('labels.slaMs') }}</span>
              <input v-model.number="biReleaseForm.slaMs" type="number" min="1" />
            </label>
            <label class="span-2">
              <span>{{ t('labels.targetConcurrency') }}</span>
              <input v-model.number="biReleaseForm.targetConcurrency" type="number" min="1" />
            </label>
          </div>

          <label class="field-block">
            <span>{{ t('labels.singleSql') }}</span>
            <textarea v-model="biReleaseForm.sql" rows="7"></textarea>
          </label>
          <label class="field-block">
            <span>{{ t('labels.dataProfileJson') }}</span>
            <textarea v-model="biReleaseForm.dataProfileJson" rows="8"></textarea>
          </label>
          <label class="field-block">
            <span>{{ t('labels.metricsJson') }}</span>
            <textarea v-model="biReleaseForm.metricsJson" rows="8"></textarea>
          </label>
          <label class="field-block">
            <span>{{ t('labels.resourceBreakdownJson') }}</span>
            <textarea v-model="biReleaseForm.resourceBreakdownJson" rows="6"></textarea>
          </label>
          <label class="field-block">
            <span>{{ t('labels.queryMixJson') }}</span>
            <textarea v-model="biReleaseForm.queryMixJson" rows="8"></textarea>
          </label>

          <div class="action-row">
            <button class="primary-button" type="button" :disabled="isRunningBiRelease" @click="runBiReleaseWorkflow">
              {{ t('buttons.biRelease') }}
            </button>
          </div>
          <p v-if="biReleaseMessage" class="status-message">{{ biReleaseMessage }}</p>
        </article>

        <article class="panel">
          <div class="panel-head">
            <div>
              <strong>{{ t('buttons.capacityPlan') }}</strong>
              <p class="card-subtitle">`POST /api/v1/workflows/capacity-plan`</p>
            </div>
          </div>

          <div class="form-grid">
            <label>
              <span>{{ t('labels.baselineQps') }}</span>
              <input v-model.number="capacityPlanForm.baselineQps" type="number" min="0.0001" step="0.0001" />
            </label>
            <label>
              <span>{{ t('labels.trafficGrowthFactor') }}</span>
              <input v-model.number="capacityPlanForm.trafficGrowthFactor" type="number" min="1" step="0.1" />
            </label>
            <label>
              <span>{{ t('labels.avgServiceTimeSec') }}</span>
              <input v-model.number="capacityPlanForm.avgServiceTimeSec" type="number" min="0.0001" step="0.0001" />
            </label>
            <label>
              <span>{{ t('labels.currentWorkers') }}</span>
              <input v-model.number="capacityPlanForm.currentWorkers" type="number" min="1" />
            </label>
            <label>
              <span>{{ t('labels.targetP99Ms') }}</span>
              <input v-model.number="capacityPlanForm.targetP99Ms" type="number" min="1" />
            </label>
            <label>
              <span>{{ t('labels.targetConcurrency') }}</span>
              <input v-model.number="capacityPlanForm.targetConcurrency" type="number" min="1" />
            </label>
            <label class="span-2">
              <span>{{ t('labels.hotDataGb') }}</span>
              <input v-model.number="capacityPlanForm.hotDataGb" type="number" min="0" step="0.1" />
            </label>
          </div>

          <label class="field-block">
            <span>{{ t('labels.queryMixJson') }}</span>
            <textarea v-model="capacityPlanForm.queryMixJson" rows="8"></textarea>
          </label>

          <div class="action-row">
            <button class="primary-button" type="button" :disabled="isRunningCapacityPlan" @click="runCapacityPlanWorkflow">
              {{ t('buttons.capacityPlan') }}
            </button>
          </div>
          <p v-if="capacityPlanMessage" class="status-message">{{ capacityPlanMessage }}</p>
        </article>

        <article class="panel">
          <div class="panel-head">
            <div>
              <p class="eyebrow">{{ t('sections.workflowBaselines') }}</p>
              <h3>{{ t('sections.workflowBaselines') }}</h3>
            </div>
            <span class="badge">{{ workflowBaselineItems.length }}</span>
          </div>

          <div class="action-row">
            <button
              class="ghost-button"
              type="button"
              :disabled="isLoadingWorkflowBaselines"
              @click="loadWorkflowBaselineHistory"
            >
              {{ t('buttons.refreshBaselines') }}
            </button>
          </div>
          <p v-if="workflowBaselineHistoryMessage" class="status-message">{{ workflowBaselineHistoryMessage }}</p>

          <template v-if="workflowBaselineItems.length">
            <div class="result-grid">
              <article v-for="item in workflowBaselineItems" :key="item.fingerprint" class="result-card">
                <div class="panel-head">
                  <div>
                    <strong>{{ item.tenantId || t('labels.noData') }}</strong>
                    <p class="card-subtitle">{{ item.updatedAt || t('labels.noData') }}</p>
                  </div>
                  <strong class="decision-pill" :class="decisionClass(item.decision)">
                    {{ workflowDecisionLabel(item.decision) }}
                  </strong>
                </div>

                <div class="definition-grid">
                  <div class="metric-card">
                    <span>{{ t('labels.fingerprint') }}</span>
                    <strong>{{ item.fingerprint || t('labels.noData') }}</strong>
                  </div>
                  <div class="metric-card">
                    <span>{{ t('labels.tenantId') }}</span>
                    <strong>{{ item.tenantId || t('labels.noData') }}</strong>
                  </div>
                  <div class="metric-card">
                    <span>{{ t('labels.decision') }}</span>
                    <strong>{{ workflowDecisionLabel(item.decision) }}</strong>
                  </div>
                  <div class="metric-card">
                    <span>{{ t('labels.lastUpdated') }}</span>
                    <strong>{{ item.updatedAt || t('labels.noData') }}</strong>
                  </div>
                  <div class="metric-card">
                    <span>P99</span>
                    <strong>{{ item.summary?.p99Ms ?? t('labels.noData') }}</strong>
                  </div>
                  <div class="metric-card">
                    <span>P50</span>
                    <strong>{{ item.summary?.p50Ms ?? item.summary?.averageLatencyMs ?? t('labels.noData') }}</strong>
                  </div>
                </div>
              </article>
            </div>
          </template>
          <p v-else class="empty-state">{{ t('empty.workflowBaselines') }}</p>
        </article>

        <article class="panel span-2">
          <div class="panel-head">
            <div>
              <strong>{{ t('buttons.planStability') }}</strong>
              <p class="card-subtitle">`POST /api/v1/workflows/plan-stability`</p>
            </div>
          </div>

          <div class="form-grid">
            <label>
              <span>{{ t('labels.tenantId') }}</span>
              <input v-model="planStabilityForm.tenantId" type="text" />
            </label>
            <label>
              <span>{{ t('labels.slaMs') }}</span>
              <input v-model.number="planStabilityForm.slaMs" type="number" min="1" />
            </label>
            <label class="span-2">
              <span>{{ t('labels.targetConcurrency') }}</span>
              <input v-model.number="planStabilityForm.targetConcurrency" type="number" min="1" />
            </label>
          </div>

          <label class="field-block">
            <span>{{ t('labels.singleSql') }}</span>
            <textarea v-model="planStabilityForm.sql" rows="7"></textarea>
          </label>
          <label class="field-block">
            <span>{{ t('labels.historicalPlanJson') }}</span>
            <textarea v-model="planStabilityForm.historicalBestPlanJson" rows="8"></textarea>
          </label>
          <label class="field-block">
            <span>{{ t('labels.currentPlanJson') }}</span>
            <textarea v-model="planStabilityForm.currentPlanJson" rows="8"></textarea>
          </label>
          <label class="field-block">
            <span>{{ t('labels.candidatePlansJson') }}</span>
            <textarea v-model="planStabilityForm.candidatePlansJson" rows="8"></textarea>
          </label>

          <div class="action-row">
            <button class="primary-button" type="button" :disabled="isRunningPlanStability" @click="runPlanStabilityWorkflow">
              {{ t('buttons.planStability') }}
            </button>
          </div>
          <p v-if="planStabilityMessage" class="status-message">{{ planStabilityMessage }}</p>
        </article>

        <article class="panel span-2">
          <div class="panel-head">
            <div>
              <p class="eyebrow">{{ t('sections.workflowResults') }}</p>
              <h3>{{ t('sections.workflowResults') }}</h3>
            </div>
          </div>
          <template v-if="workflowPanels.length">
            <div class="result-grid">
              <article v-for="panel in workflowPanels" :key="panel.id" class="result-card">
                <div class="panel-head">
                  <div>
                    <strong>{{ panel.title }}</strong>
                    <p v-if="panel.message" class="card-subtitle">{{ panel.message }}</p>
                  </div>
                  <span v-if="panel.badge" class="badge">{{ panel.badge }}</span>
                </div>
                <pre class="json-block">{{ formatJson(panel.result) }}</pre>
              </article>
            </div>
          </template>
          <p v-else class="empty-state">{{ t('empty.workflows') }}</p>
        </article>
      </section>

      <section v-else-if="activeMenu === 'pressure'" class="panel-grid">
        <article class="panel span-2">
          <div class="panel-head">
            <div>
              <p class="eyebrow">{{ t('sections.pressureWorkbench') }}</p>
              <h3>{{ t('sections.pressureWorkbench') }}</h3>
            </div>
          </div>
          <p class="section-note">{{ t('hints.pressure') }}</p>

          <div class="form-grid">
            <label>
              <span>{{ t('labels.source') }}</span>
              <input v-model="sqlIntentSource" type="text" />
            </label>
            <label>
              <span>{{ t('labels.batchId') }}</span>
              <input v-model="sqlIntentBatchId" type="text" />
            </label>
            <label>
              <span>{{ t('labels.targetConcurrency') }}</span>
              <input v-model.number="sqlPressureTargetConcurrency" type="number" min="1" max="500" />
            </label>
          </div>

          <label class="field-block">
            <span>{{ t('labels.batchSql') }}</span>
            <textarea v-model="sqlIntentBatchInput" rows="12"></textarea>
          </label>

          <div class="action-row">
            <button class="primary-button" type="button" :disabled="isBuildingPressurePlan" @click="buildSqlPressurePlan">
              {{ t('buttons.pressurePlan') }}
            </button>
            <button class="ghost-button" type="button" :disabled="isBuildingScenarioBlueprint" @click="buildSqlScenarioBlueprint">
              {{ t('buttons.blueprint') }}
            </button>
            <button class="ghost-button" type="button" :disabled="isBuildingExecutionManifest" @click="buildSqlExecutionManifest">
              {{ t('buttons.manifest') }}
            </button>
            <button class="ghost-button" type="button" :disabled="isBuildingCampaignSchedule" @click="buildSqlCampaignSchedule">
              {{ t('buttons.schedule') }}
            </button>
          </div>
        </article>

        <article class="panel span-2">
          <div class="panel-head">
            <div>
              <p class="eyebrow">{{ t('sections.pressureResults') }}</p>
              <h3>{{ t('sections.pressureResults') }}</h3>
            </div>
          </div>
          <template v-if="pressurePanels.length">
            <div class="result-grid">
              <article v-for="panel in pressurePanels" :key="panel.id" class="result-card">
                <div class="panel-head">
                  <div>
                    <strong>{{ panel.title }}</strong>
                    <p v-if="panel.message" class="card-subtitle">{{ panel.message }}</p>
                  </div>
                  <span v-if="panel.badge" class="badge">{{ panel.badge }}</span>
                </div>
                <pre v-if="panel.result" class="json-block">{{ formatJson(panel.result) }}</pre>
              </article>
            </div>
          </template>
          <p v-else class="empty-state">{{ t('empty.pressure') }}</p>
        </article>
      </section>

      <section v-else class="panel-grid">
        <article class="panel span-2">
          <div class="panel-head">
            <div>
              <p class="eyebrow">{{ t('sections.handoffWorkbench') }}</p>
              <h3>{{ t('sections.handoffWorkbench') }}</h3>
            </div>
          </div>
          <p class="section-note">{{ t('hints.handoff') }}</p>

          <div class="form-grid">
            <label>
              <span>{{ t('labels.source') }}</span>
              <input v-model="sqlIntentSource" type="text" />
            </label>
            <label>
              <span>{{ t('labels.batchId') }}</span>
              <input v-model="sqlIntentBatchId" type="text" />
            </label>
            <label>
              <span>{{ t('labels.targetConcurrency') }}</span>
              <input v-model.number="sqlPressureTargetConcurrency" type="number" min="1" max="500" />
            </label>
          </div>

          <label class="field-block">
            <span>{{ t('labels.batchSql') }}</span>
            <textarea v-model="sqlIntentBatchInput" rows="12"></textarea>
          </label>

          <div class="action-row">
            <button class="primary-button" type="button" :disabled="isBuildingRunPackage" @click="buildSqlRunPackage">
              {{ t('buttons.runPackage') }}
            </button>
            <button class="ghost-button" type="button" :disabled="isBuildingBriefingReport" @click="buildSqlBriefingReport">
              {{ t('buttons.briefing') }}
            </button>
            <button class="ghost-button" type="button" :disabled="isBuildingReadinessGate" @click="buildSqlReadinessGate">
              {{ t('buttons.gate') }}
            </button>
          </div>
        </article>

        <article class="panel span-2">
          <div class="panel-head">
            <div>
              <p class="eyebrow">{{ t('sections.handoffResults') }}</p>
              <h3>{{ t('sections.handoffResults') }}</h3>
            </div>
          </div>
          <template v-if="handoffPanels.length">
            <div class="result-grid">
              <article v-for="panel in handoffPanels" :key="panel.id" class="result-card" :class="{ 'gate-card': panel.id === 'readiness-gate' }">
                <div class="panel-head">
                  <div>
                    <strong>{{ panel.title }}</strong>
                    <p v-if="panel.message" class="card-subtitle">{{ panel.message }}</p>
                  </div>
                  <span v-if="panel.badge" class="badge">{{ panel.badge }}</span>
                </div>

                <template v-if="panel.id === 'readiness-gate' && panel.result">
                  <div class="gate-summary">
                    <div class="metric-card">
                      <span>{{ t('labels.decision') }}</span>
                      <strong class="decision-pill" :class="decisionClass(panel.result.decision)">
                        {{ readinessDecisionLabel(panel.result.decision) }}
                      </strong>
                    </div>
                    <div class="metric-card">
                      <span>{{ t('labels.parsedStatements') }}</span>
                      <strong>{{ panel.result.parsedStatementCount ?? 0 }}</strong>
                    </div>
                  </div>
                  <div class="gate-columns">
                    <div>
                      <h4>{{ t('labels.blockers') }}</h4>
                      <ul class="bullet-list">
                        <li v-for="item in panel.result.blockers || []" :key="`${item.code}-${item.message}`">
                          {{ item.severity }} · {{ item.message }}
                        </li>
                      </ul>
                    </div>
                    <div>
                      <h4>{{ t('labels.prerequisites') }}</h4>
                      <ul class="bullet-list">
                        <li v-for="item in panel.result.prerequisites || []" :key="item">{{ item }}</li>
                      </ul>
                    </div>
                    <div>
                      <h4>{{ t('labels.recommendations') }}</h4>
                      <ul class="bullet-list">
                        <li v-for="item in panel.result.recommendations || []" :key="item">{{ item }}</li>
                      </ul>
                    </div>
                  </div>
                </template>

                <pre v-if="panel.result" class="json-block">{{ formatJson(panel.result) }}</pre>
              </article>
            </div>
          </template>
          <p v-else class="empty-state">{{ t('empty.handoff') }}</p>
        </article>
      </section>
    </section>
  </main>
</template>
