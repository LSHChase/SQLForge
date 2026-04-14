<script setup>
import { onMounted, ref, watch } from 'vue';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
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
const isSubmitting = ref(false);
const isProbing = ref(false);
const isPreviewing = ref(false);
const isAnalyzingIntent = ref(false);
const selectedConnectionId = ref('');
const previewSql = ref('select 1 as health_check');
const previewMaxRows = ref(20);
const sqlIntentSource = ref('manual-sample');
const sqlIntentInput = ref(
  "with recent_orders as (select user_id, amount from lake.orders where ds >= '2026-04-01') "
    + "select user_id, sum(amount) from recent_orders group by 1 order by sum(amount) desc"
);
const form = ref({
  name: 'Primary Trino',
  engineCode: 'trino',
  host: 'trino.sqlforge.local',
  port: 8443,
  catalog: 'lakehouse',
  username: 'analyst',
  password: 'changeit',
  sslEnabled: true
});

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

function applyEngineDefaults(engineCode) {
  const selectedEngine = engines.value.find((engine) => engine.code === engineCode);

  if (!selectedEngine) {
    return;
  }

  form.value.port = selectedEngine.defaultPort;
}

async function loadSystemState() {
  try {
    const [healthResponse, enginesResponse, driverAuditResponse, overviewResponse, connectionsResponse] = await Promise.all([
      fetch(`${apiBaseUrl}/api/v1/system/health`),
      fetch(`${apiBaseUrl}/api/v1/system/engines`),
      fetch(`${apiBaseUrl}/api/v1/system/driver-audit`),
      fetch(`${apiBaseUrl}/api/v1/system/connection-overview`),
      fetch(`${apiBaseUrl}/api/v1/connections`)
    ]);

    if (!healthResponse.ok || !enginesResponse.ok || !driverAuditResponse.ok || !overviewResponse.ok || !connectionsResponse.ok) {
      throw new Error('backend service returned an unexpected response');
    }

    const healthPayload = await healthResponse.json();
    const enginesPayload = await enginesResponse.json();
    const driverAuditPayload = await driverAuditResponse.json();
    const overviewPayload = await overviewResponse.json();
    const connectionsPayload = await connectionsResponse.json();

    health.value = healthPayload.status;
    engines.value = enginesPayload.engines;
    driverAudit.value = driverAuditPayload.drivers;
    overview.value = overviewPayload.overview;
    connections.value = connectionsPayload.connections;
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

function loadConnection(connection) {
  selectedConnectionId.value = connection.id;
  form.value = {
    name: connection.name,
    engineCode: connection.engineCode,
    host: connection.host,
    port: connection.port,
    catalog: connection.catalog,
    username: connection.username,
    password: '',
    sslEnabled: connection.sslEnabled
  };
  saveMessage.value = `已加载连接：${connection.name}，请重新输入密码后执行探测或 SQL 预览`;
  loadConnectionActivity(connection.id);
}

function resetEditor() {
  selectedConnectionId.value = '';
  connectionActivity.value = [];
  form.value = {
    name: 'Primary Trino',
    engineCode: 'trino',
    host: 'trino.sqlforge.local',
    port: 8443,
    catalog: 'lakehouse',
    username: 'analyst',
    password: 'changeit',
    sslEnabled: true
  };
  saveMessage.value = '已切换到新建模式';
}

async function loadConnectionActivity(connectionId) {
  try {
    const response = await fetch(`${apiBaseUrl}/api/v1/connections/${connectionId}/activity`);

    if (!response.ok) {
      throw new Error('failed to load connection activity');
    }

    const payload = await response.json();
    connectionActivity.value = payload.activity;
  } catch (error) {
    connectionActivity.value = [];
  }
}

async function validateConnection() {
  validationMessage.value = '';

  try {
    const response = await fetch(`${apiBaseUrl}/api/v1/connections/validate`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(form.value)
    });

    const payload = await response.json();

    if (!response.ok) {
      throw new Error(payload.message || 'validation failed');
    }

    validationMessage.value = payload.validation.messages.join('；');
  } catch (error) {
    validationMessage.value = error.message;
  }
}

async function createConnection() {
  saveMessage.value = '';
  isSubmitting.value = true;

  try {
    const isUpdate = Boolean(selectedConnectionId.value);
    const response = await fetch(
      isUpdate
        ? `${apiBaseUrl}/api/v1/connections/${selectedConnectionId.value}`
        : `${apiBaseUrl}/api/v1/connections`,
      {
        method: isUpdate ? 'PUT' : 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(form.value)
      }
    );

    const payload = await response.json();

    if (!response.ok) {
      throw new Error(payload.message || 'create connection failed');
    }

    if (isUpdate) {
      connections.value = connections.value.map((connection) =>
        connection.id === payload.connection.id ? payload.connection : connection
      );
      saveMessage.value = `连接已更新：${payload.connection.name}`;
      loadConnectionActivity(payload.connection.id);
    } else {
      connections.value = [payload.connection, ...connections.value];
      saveMessage.value = `连接已登记并持久化元数据：${payload.connection.name}`;
      loadConnectionActivity(payload.connection.id);
      selectedConnectionId.value = payload.connection.id;
    }
    loadSystemState();
  } catch (error) {
    saveMessage.value = error.message;
  } finally {
    isSubmitting.value = false;
  }
}

async function deleteConnection(connectionId) {
  saveMessage.value = '';

  try {
    const response = await fetch(`${apiBaseUrl}/api/v1/connections/${connectionId}`, {
      method: 'DELETE'
    });

    const payload = await response.json();

    if (!response.ok) {
      throw new Error(payload.message || 'delete connection failed');
    }

    connections.value = connections.value.filter((connection) => connection.id !== connectionId);
    if (selectedConnectionId.value === connectionId) {
      resetEditor();
    }
    saveMessage.value = `连接已删除：${connectionId}`;
    loadSystemState();
  } catch (error) {
    saveMessage.value = error.message;
  }
}

async function probeConnection() {
  probeMessage.value = '';
  probeResult.value = null;
  isProbing.value = true;

  try {
    const response = await fetch(`${apiBaseUrl}/api/v1/connections/probe`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        ...form.value,
        connectionId: selectedConnectionId.value || undefined
      })
    });

    const payload = await response.json();

    if (!response.ok) {
      throw new Error(payload.message || 'probe failed');
    }

    probeResult.value = payload.probe;
    probeMessage.value = payload.probe.messages.join('；');
    if (payload.connection) {
      connections.value = connections.value.map((connection) =>
        connection.id === payload.connection.id ? payload.connection : connection
      );
      loadConnectionActivity(payload.connection.id);
      loadSystemState();
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
    const response = await fetch(`${apiBaseUrl}/api/v1/connections/query-preview`, {
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

    const payload = await response.json();

    if (!response.ok) {
      throw new Error(payload.message || 'query preview failed');
    }

    previewResult.value = payload.preview;
    previewMessage.value = payload.preview.messages.join('；');
    if (payload.connection) {
      connections.value = connections.value.map((connection) =>
        connection.id === payload.connection.id ? payload.connection : connection
      );
      loadConnectionActivity(payload.connection.id);
      loadSystemState();
    }
  } catch (error) {
    previewMessage.value = error.message;
  } finally {
    isPreviewing.value = false;
  }
}

async function analyzeSqlIntent() {
  sqlIntentMessage.value = '';
  sqlIntentResult.value = null;
  isAnalyzingIntent.value = true;

  try {
    const response = await fetch(`${apiBaseUrl}/api/v1/sql/intent-analysis`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        statements: [
          {
            id: 'manual-analysis',
            source: sqlIntentSource.value,
            sql: sqlIntentInput.value
          }
        ]
      })
    });

    const payload = await response.json();

    if (!response.ok) {
      throw new Error(payload.message || 'sql intent analysis failed');
    }

    sqlIntentResult.value = payload;
    sqlIntentMessage.value = '结构分析已完成，结果仅基于 SQL 形态，不依赖数据库执行。';
  } catch (error) {
    sqlIntentMessage.value = error.message;
  } finally {
    isAnalyzingIntent.value = false;
  }
}

onMounted(() => {
  loadSystemState();
});

watch(
  () => form.value.engineCode,
  (engineCode) => {
    applyEngineDefaults(engineCode);
  }
);
</script>

<template>
  <main class="page-shell">
    <section class="hero-panel">
      <p class="eyebrow">SQLForge</p>
      <h1>面向多引擎 SQL 平台的前后端分离控制台</h1>
      <p class="hero-copy">
        前端采用 Vue + JavaScript + CSS，后端采用 Java 8 + Spring Boot。默认工程规范为
        UTF-8、Unix/LF，并面向 ARM 与 x86 双架构部署。
      </p>
      <div class="hero-meta">
        <span>Backend: {{ health }}</span>
        <span>API: {{ apiBaseUrl }}</span>
        <span>Storage: metadata persisted, secrets excluded</span>
      </div>
      <p v-if="errorMessage" class="status-hint">
        当前展示的是本地回退引擎清单，因为后端接口尚未连通：{{ errorMessage }}
      </p>
    </section>

    <section class="content-grid">
      <article class="card">
        <h2>基础交付要求</h2>
        <ul>
          <li>前后端分离</li>
          <li>Vue + JS + CSS 前端</li>
          <li>Java 8 + Spring Boot 后端</li>
          <li>UTF-8 / Unix(LF)</li>
          <li>ARM 架构兼容</li>
        </ul>
      </article>

      <article class="card">
        <h2>支持的数据引擎</h2>
        <div class="engine-list">
          <div v-for="engine in engines" :key="engine.code" class="engine-pill">
            <strong>{{ engine.name }}</strong>
            <span>{{ engine.category }} · {{ engine.transport }} · {{ engine.defaultPort }}</span>
          </div>
        </div>
        <div class="driver-audit-list">
          <div v-for="item in driverAudit" :key="item.engineCode" class="driver-audit-item">
            <strong>{{ item.engineName }}</strong>
            <span>{{ item.available ? 'driver-ready' : 'driver-missing' }}</span>
          </div>
        </div>
      </article>
    </section>

    <section v-if="overview" class="content-grid">
      <article class="card">
        <h2>连接模块总览</h2>
        <div class="overview-grid">
          <div class="overview-item">
            <strong>{{ overview.totalConnections }}</strong>
            <span>saved connections</span>
          </div>
          <div class="overview-item">
            <strong>{{ overview.driverReadyCount }}</strong>
            <span>drivers ready</span>
          </div>
          <div class="overview-item">
            <strong>{{ overview.lastProbeHealthyCount }}</strong>
            <span>healthy probe</span>
          </div>
          <div class="overview-item">
            <strong>{{ overview.lastPreviewHealthyCount }}</strong>
            <span>healthy preview</span>
          </div>
        </div>
      </article>

      <article class="card">
        <h2>按引擎分布</h2>
        <div v-if="overview.engines && overview.engines.length" class="driver-audit-list">
          <div v-for="item in overview.engines" :key="item.engineCode" class="driver-audit-item">
            <strong>{{ item.engineCode }}</strong>
            <span>{{ item.connectionCount }} connections</span>
          </div>
        </div>
        <p v-else class="empty-state">当前还没有保存的连接。</p>
      </article>
    </section>

    <section class="content-grid analysis-section">
      <article class="card analysis-card">
        <div class="section-head">
          <div>
            <p class="section-kicker">Structure Analysis</p>
            <h2>SQL 意图识别</h2>
          </div>
          <button class="primary-button" type="button" :disabled="isAnalyzingIntent" @click="analyzeSqlIntent">
            {{ isAnalyzingIntent ? '分析中...' : '结构分析' }}
          </button>
        </div>

        <p class="info-text">
          面向压测前准备的纯结构分析能力。当前只看 SQL 文本形态，不连接数据库、不执行查询。
        </p>

        <label class="preview-label compact-label">
          <span>样本来源</span>
          <input v-model="sqlIntentSource" type="text" />
        </label>
        <label class="preview-label">
          <span>输入 SQL</span>
          <textarea v-model="sqlIntentInput" rows="8"></textarea>
        </label>

        <p v-if="sqlIntentMessage" class="info-text">{{ sqlIntentMessage }}</p>

        <div v-if="sqlIntentResult" class="analysis-panel">
          <div class="probe-head">
            <strong>Analysis Summary</strong>
            <span class="badge">{{ sqlIntentResult.summary.statementCount }} statements</span>
          </div>

          <div class="analysis-grid">
            <div class="overview-item">
              <strong>{{ Object.keys(sqlIntentResult.summary.byLoadClass || {}).join(', ') || 'n/a' }}</strong>
              <span>load class</span>
            </div>
            <div class="overview-item">
              <strong>{{ Object.keys(sqlIntentResult.summary.byIntentTag || {}).length }}</strong>
              <span>intent tags</span>
            </div>
          </div>

          <div
            v-for="item in sqlIntentResult.analyses"
            :key="item.statementId"
            class="preview-panel"
          >
            <div class="probe-head">
              <strong>{{ item.statementId }}</strong>
              <span class="badge">{{ item.pressureProfile.loadClass }}</span>
            </div>
            <p class="probe-line"><strong>Fingerprint:</strong> {{ item.fingerprint }}</p>
            <p class="probe-line"><strong>Statement Type:</strong> {{ item.structure.statementType }}</p>
            <p class="probe-line"><strong>Tables:</strong> {{ item.structure.tables.join(', ') || 'n/a' }}</p>
            <p class="probe-line"><strong>Join Type:</strong> {{ item.structure.joinType }}</p>
            <p class="probe-line">
              <strong>Complexity:</strong> {{ item.pressureProfile.complexityTier }} ({{ item.pressureProfile.complexityScore }})
            </p>

            <div class="tag-list">
              <span v-for="tag in item.intentTags" :key="tag" class="tag-chip">{{ tag }}</span>
            </div>

            <div class="tag-list">
              <span v-for="signal in item.pressureProfile.pressureSignals" :key="signal" class="signal-chip">
                {{ signal }}
              </span>
            </div>

            <div v-if="item.structuralAlerts && item.structuralAlerts.length" class="alert-list">
              <div v-for="(alert, index) in item.structuralAlerts" :key="index" class="alert-item">
                <strong>{{ alert.code }}</strong>
                <span>{{ alert.message }}</span>
              </div>
            </div>
          </div>
        </div>
      </article>
    </section>

    <section class="workspace-grid">
      <article class="card connection-card">
        <div class="section-head">
          <div>
            <p class="section-kicker">Connection Studio</p>
            <h2>{{ selectedConnectionId ? '编辑已登记连接' : '新增数据引擎连接' }}</h2>
          </div>
          <div class="button-stack">
            <button class="ghost-button" type="button" @click="resetEditor">新建模式</button>
            <button class="ghost-button" type="button" @click="validateConnection">离线校验</button>
            <button class="ghost-button" type="button" :disabled="isProbing" @click="probeConnection">
              {{ isProbing ? '探测中...' : '连通性探测' }}
            </button>
            <button class="ghost-button" type="button" :disabled="isPreviewing" @click="previewQuery">
              {{ isPreviewing ? '预览中...' : 'SQL 预览' }}
            </button>
          </div>
        </div>

        <div class="form-grid">
          <label>
            <span>连接名称</span>
            <input v-model="form.name" type="text" />
          </label>
          <label>
            <span>数据引擎</span>
            <select v-model="form.engineCode">
              <option v-for="engine in engines" :key="engine.code" :value="engine.code">
                {{ engine.name }}
              </option>
            </select>
          </label>
          <label>
            <span>主机地址</span>
            <input v-model="form.host" type="text" />
          </label>
          <label>
            <span>端口</span>
            <input v-model.number="form.port" type="number" min="1" max="65535" />
          </label>
          <label>
            <span>Catalog / Database</span>
            <input v-model="form.catalog" type="text" />
          </label>
          <label>
            <span>用户名</span>
            <input v-model="form.username" type="text" />
          </label>
          <label class="full-span">
            <span>密码</span>
            <input v-model="form.password" type="password" />
          </label>
        </div>

        <label class="switch-row">
          <input v-model="form.sslEnabled" type="checkbox" />
          <span>启用 SSL/TLS</span>
        </label>

        <p class="info-text muted-text">
          当前版本只持久化连接元数据，密码不会写入返回结果或落盘文件。
        </p>
        <p class="info-text muted-text" v-if="engines.length">
          当前引擎档案：{{
            (engines.find((engine) => engine.code === form.engineCode) || {}).profileNote
          }}
        </p>
        <p class="info-text muted-text" v-if="engines.length">
          预期驱动类：{{
            (engines.find((engine) => engine.code === form.engineCode) || {}).driverClassName
          }}
        </p>

        <div class="action-row">
          <button class="primary-button" type="button" :disabled="isSubmitting" @click="createConnection">
            {{ isSubmitting ? '提交中...' : selectedConnectionId ? '更新连接' : '保存连接' }}
          </button>
          <p v-if="validationMessage" class="info-text">{{ validationMessage }}</p>
          <p v-if="probeMessage" class="info-text">{{ probeMessage }}</p>
          <p v-if="previewMessage" class="info-text">{{ previewMessage }}</p>
          <p v-if="saveMessage" class="success-text">{{ saveMessage }}</p>
        </div>

        <div v-if="probeResult" class="probe-panel">
          <div class="probe-head">
            <strong>Probe Result</strong>
            <span :class="probeResult.reachable ? 'probe-ok' : 'probe-fail'">
              {{ probeResult.status }}
            </span>
          </div>
          <p class="probe-line"><strong>JDBC URL:</strong> {{ probeResult.jdbcUrl }}</p>
          <p class="probe-line"><strong>传输层:</strong> {{ probeResult.transportStatus }}</p>
          <p class="probe-line"><strong>驱动层:</strong> {{ probeResult.driverStatus }}</p>
          <p class="probe-line"><strong>驱动类:</strong> {{ probeResult.driverClassName || 'n/a' }}</p>
          <p class="probe-line"><strong>JDBC 尝试:</strong> {{ probeResult.jdbcAttempted ? 'yes' : 'no' }}</p>
          <p class="probe-line"><strong>耗时:</strong> {{ probeResult.durationMs }} ms</p>
        </div>

        <div class="preview-panel">
          <div class="probe-head">
            <strong>SQL Preview</strong>
            <span class="badge">{{ previewMaxRows }} rows max</span>
          </div>
          <label class="preview-label">
            <span>预览 SQL</span>
            <textarea v-model="previewSql" rows="6"></textarea>
          </label>
          <label class="preview-label compact-label">
            <span>最大返回行数</span>
            <input v-model.number="previewMaxRows" type="number" min="1" max="100" />
          </label>

          <div v-if="previewResult" class="preview-result">
            <p class="probe-line"><strong>状态:</strong> {{ previewResult.status }}</p>
            <p class="probe-line"><strong>驱动层:</strong> {{ previewResult.driverStatus }}</p>
            <p class="probe-line"><strong>JDBC URL:</strong> {{ previewResult.jdbcUrl }}</p>
            <p class="probe-line"><strong>返回行数:</strong> {{ previewResult.rowCount }}</p>
            <p class="probe-line"><strong>耗时:</strong> {{ previewResult.durationMs }} ms</p>

            <div v-if="previewResult.columns && previewResult.columns.length" class="preview-table">
              <table>
                <thead>
                  <tr>
                    <th v-for="column in previewResult.columns" :key="column">{{ column }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, rowIndex) in previewResult.rows" :key="rowIndex">
                    <td v-for="column in previewResult.columns" :key="column">{{ row[column] }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <div v-if="selectedConnectionId" class="preview-panel">
          <div class="probe-head">
            <strong>Connection Activity</strong>
            <span class="badge">{{ connectionActivity.length }} events</span>
          </div>
          <div v-if="connectionActivity.length" class="activity-list">
            <div v-for="item in connectionActivity" :key="item.id" class="activity-item">
              <strong>{{ item.actionType }}</strong>
              <span>{{ item.actionStatus }}</span>
              <span>{{ item.createdAt }}</span>
            </div>
          </div>
          <p v-else class="empty-state">当前连接还没有活动记录。</p>
        </div>
      </article>

      <article class="card">
        <div class="section-head">
          <div>
            <p class="section-kicker">Registry</p>
            <h2>已登记连接</h2>
          </div>
          <span class="badge">{{ connections.length }} entries</span>
        </div>

        <div v-if="connections.length" class="connection-list">
          <div v-for="connection in connections" :key="connection.id" class="connection-item">
            <div>
              <strong>{{ connection.name }}</strong>
              <p>{{ connection.engineCode }} · {{ connection.host }}:{{ connection.port }}</p>
            </div>
            <div class="connection-meta">
              <span>{{ connection.catalog }}</span>
              <span>{{ connection.status }}</span>
              <span>{{ connection.createdAt }}</span>
              <span v-if="connection.lastProbeStatus">
                probe: {{ connection.lastProbeStatus }}
              </span>
              <span v-if="connection.lastPreviewStatus">
                preview: {{ connection.lastPreviewStatus }}
              </span>
            </div>
            <div class="connection-actions">
              <button class="ghost-button" type="button" @click="loadConnection(connection)">加载</button>
              <button class="danger-button" type="button" @click="deleteConnection(connection.id)">删除</button>
            </div>
          </div>
        </div>
        <p v-else class="empty-state">当前还没有登记连接。</p>
      </article>
    </section>
  </main>
</template>
