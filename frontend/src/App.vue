<script setup>
import { onMounted, ref } from 'vue';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
const health = ref('checking');
const engines = ref([]);
const connections = ref([]);
const errorMessage = ref('');
const saveMessage = ref('');
const validationMessage = ref('');
const probeMessage = ref('');
const probeResult = ref(null);
const isSubmitting = ref(false);
const isProbing = ref(false);
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
  { code: 'mysql', name: 'MySQL', category: 'oltp' },
  { code: 'trino', name: 'Trino', category: 'query-engine' },
  { code: 'presto', name: 'Presto', category: 'query-engine' },
  { code: 'clickhouse', name: 'ClickHouse', category: 'olap' },
  { code: 'mrs-hetu', name: 'MRS-Hetu', category: 'query-engine' },
  { code: 'kyligence', name: 'Kyligence', category: 'cube-engine' }
];

async function loadSystemState() {
  try {
    const [healthResponse, enginesResponse, connectionsResponse] = await Promise.all([
      fetch(`${apiBaseUrl}/api/v1/system/health`),
      fetch(`${apiBaseUrl}/api/v1/system/engines`),
      fetch(`${apiBaseUrl}/api/v1/connections`)
    ]);

    if (!healthResponse.ok || !enginesResponse.ok || !connectionsResponse.ok) {
      throw new Error('backend service returned an unexpected response');
    }

    const healthPayload = await healthResponse.json();
    const enginesPayload = await enginesResponse.json();
    const connectionsPayload = await connectionsResponse.json();

    health.value = healthPayload.status;
    engines.value = enginesPayload.engines;
    connections.value = connectionsPayload.connections;
  } catch (error) {
    health.value = 'offline';
    engines.value = fallbackEngines;
    connections.value = [];
    errorMessage.value = error.message;
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
    const response = await fetch(`${apiBaseUrl}/api/v1/connections`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(form.value)
    });

    const payload = await response.json();

    if (!response.ok) {
      throw new Error(payload.message || 'create connection failed');
    }

    connections.value = [payload.connection, ...connections.value];
    saveMessage.value = `连接已登记并持久化元数据：${payload.connection.name}`;
  } catch (error) {
    saveMessage.value = error.message;
  } finally {
    isSubmitting.value = false;
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
      body: JSON.stringify(form.value)
    });

    const payload = await response.json();

    if (!response.ok) {
      throw new Error(payload.message || 'probe failed');
    }

    probeResult.value = payload.probe;
    probeMessage.value = payload.probe.messages.join('；');
  } catch (error) {
    probeMessage.value = error.message;
  } finally {
    isProbing.value = false;
  }
}

onMounted(() => {
  loadSystemState();
});
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
            <span>{{ engine.category }}</span>
          </div>
        </div>
      </article>
    </section>

    <section class="workspace-grid">
      <article class="card connection-card">
        <div class="section-head">
          <div>
            <p class="section-kicker">Connection Studio</p>
            <h2>新增数据引擎连接</h2>
          </div>
          <div class="button-stack">
            <button class="ghost-button" type="button" @click="validateConnection">离线校验</button>
            <button class="ghost-button" type="button" :disabled="isProbing" @click="probeConnection">
              {{ isProbing ? '探测中...' : '连通性探测' }}
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

        <div class="action-row">
          <button class="primary-button" type="button" :disabled="isSubmitting" @click="createConnection">
            {{ isSubmitting ? '提交中...' : '保存连接' }}
          </button>
          <p v-if="validationMessage" class="info-text">{{ validationMessage }}</p>
          <p v-if="probeMessage" class="info-text">{{ probeMessage }}</p>
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
          <p class="probe-line"><strong>耗时:</strong> {{ probeResult.durationMs }} ms</p>
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
            </div>
          </div>
        </div>
        <p v-else class="empty-state">当前还没有登记连接。</p>
      </article>
    </section>
  </main>
</template>
