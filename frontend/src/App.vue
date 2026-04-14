<script setup>
import { onMounted, ref } from 'vue';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
const health = ref('checking');
const engines = ref([]);
const errorMessage = ref('');

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
    const [healthResponse, enginesResponse] = await Promise.all([
      fetch(`${apiBaseUrl}/api/v1/system/health`),
      fetch(`${apiBaseUrl}/api/v1/system/engines`)
    ]);

    if (!healthResponse.ok || !enginesResponse.ok) {
      throw new Error('backend service returned an unexpected response');
    }

    const healthPayload = await healthResponse.json();
    const enginesPayload = await enginesResponse.json();

    health.value = healthPayload.status;
    engines.value = enginesPayload.engines;
  } catch (error) {
    health.value = 'offline';
    engines.value = fallbackEngines;
    errorMessage.value = error.message;
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
  </main>
</template>
