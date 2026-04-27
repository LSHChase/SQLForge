<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const { locale } = useI18n()
const isChinese = computed(() => locale.value === 'zh-CN')

const inventoryItems = computed(() => [
  {
    title: 'MySQL / Core Traceability',
    summary: isChinese.value
      ? '主库、核心追溯链、schema 版本与 migration 清单必须成批恢复。'
      : 'Primary metadata tables, schema version, and migration inventory must recover as one batch.'
  },
  {
    title: 'audit_log',
    summary: isChinese.value
      ? '审计留痕必须连续，`LOGIN/LOGOUT` 与 `audit/write` 抽样恢复后仍可落库。'
      : 'Audit evidence must remain continuous and still accept `LOGIN/LOGOUT` and `audit/write` samples after restore.'
  },
  {
    title: 'export_record / archive pointers',
    summary: isChinese.value
      ? '导出元数据和脱敏归档索引要能互相核对。'
      : 'Export metadata and sanitized archive pointers must reconcile with each other.'
  },
  {
    title: 'kafka_message_queue',
    summary: isChinese.value
      ? '数据库兜底或 fallback backlog 恢复后必须还能继续补偿。'
      : 'Database fallback backlog must remain replayable after recovery.'
  },
  {
    title: 'system_config / key boundary',
    summary: isChinese.value
      ? '只恢复密文、不回流明文，`encryption_key_id` 必须与批次对应。'
      : 'Restore ciphertext only, never plaintext, and keep `encryption_key_id` aligned with the backup batch.'
  }
])

const objectiveRows = computed(() => [
  ['governance / core metadata', '< 1h', '< 4h', 'DBA / Platform Ops'],
  ['governance / audit_log', '< 1h', '< 4h', 'DBA / Compliance Ops'],
  ['governance / export_record', '< 1h', '< 4h', 'DBA / Storage Ops'],
  ['governance / kafka_message_queue', '< 1h', '< 4h', 'DBA / Messaging Ops'],
  ['governance / system_config + keys', isChinese.value ? '与备份批次同步' : 'Same batch as backup', isChinese.value ? '与备份批次同步' : 'Same batch as backup', 'Security Ops']
])

const checklistItems = computed(() => [
  isChinese.value ? '4 个后端 `/actuator/health` 和治理 `/api/governance/health` 全部返回 `UP`。' : 'All backend `/actuator/health` probes and governance `/api/governance/health` return `UP`.',
  isChinese.value ? '恢复后复跑 `audit/write` 抽样、`LOGIN/LOGOUT` 审计样本。' : 'Replay `audit/write` and `LOGIN/LOGOUT` audit samples after restore.',
  isChinese.value ? '检查 `kafka_message_queue` backlog 或明确记录为何不适用。' : 'Check `kafka_message_queue` backlog or explicitly record why it is not applicable.',
  isChinese.value ? '抽样 `export_record` 与 `history_id/result_id` 追溯键，确认脱敏地址未泄漏。' : 'Sample `export_record` against `history_id/result_id` and confirm storage pointers remain sanitized.',
  isChinese.value ? '抽检日志平台和 `system_config`，确认没有密码、Token、密钥明文泄漏。' : 'Sample log platforms and `system_config` to confirm no password, token, or key leaks.'
])
</script>

<template>
  <section class="runtime-page" data-testid="recovery-drill-page">
    <div class="runtime-hero surface-card">
      <div>
        <p class="runtime-eyebrow sqlforge-code-label">recovery drill baseline</p>
        <h1 class="runtime-title">
          {{ isChinese ? '备份恢复与恢复后验收基线' : 'Backup Recovery And Post-Restore Acceptance Baseline' }}
        </h1>
        <p class="runtime-summary">
          {{
            isChinese
              ? '这一页把 F-TASK-008/009 形成的备份对象、恢复目标、责任边界和恢复后检查统一收在治理运维路径里。'
              : 'This page gathers the backup inventory, recovery objectives, ownership boundaries, and post-restore checks established by F-TASK-008/009.'
          }}
        </p>
      </div>
      <p class="runtime-note">
        {{
          isChinese
            ? '恢复完成的定义不是库导回来了，而是健康探针、审计补偿、队列 backlog、导出/脱敏和敏感泄漏检查都通过。'
            : 'Restore completion means more than database replay: health probes, audit compensation, queue backlog, export/desensitization, and leak checks must all pass.'
        }}
      </p>
    </div>

    <div class="summary-card-grid">
      <article
        v-for="item in inventoryItems"
        :key="item.title"
        class="surface-card inventory-card"
      >
        <p class="section-kicker sqlforge-code-label">{{ item.title }}</p>
        <h2 class="section-title">{{ item.title }}</h2>
        <p class="section-summary">{{ item.summary }}</p>
      </article>
    </div>

    <div class="runtime-grid">
      <article class="surface-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">rpo / rto</p>
            <h2 class="section-title">{{ isChinese ? '恢复目标与责任人' : 'Recovery Objectives And Owners' }}</h2>
          </div>
        </div>

        <div class="table-wrap">
          <table class="objective-table">
            <thead>
              <tr>
                <th>{{ isChinese ? '数据域' : 'Domain' }}</th>
                <th>RPO</th>
                <th>RTO</th>
                <th>{{ isChinese ? '责任人' : 'Owner' }}</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="row in objectiveRows"
                :key="row[0]"
              >
                <td>{{ row[0] }}</td>
                <td>{{ row[1] }}</td>
                <td>{{ row[2] }}</td>
                <td>{{ row[3] }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <article class="surface-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">acceptance checklist</p>
            <h2 class="section-title">{{ isChinese ? '恢复后必须复验的清单' : 'Mandatory Post-Restore Checklist' }}</h2>
          </div>
        </div>

        <ul class="bullet-list">
          <li
            v-for="item in checklistItems"
            :key="item"
          >
            {{ item }}
          </li>
        </ul>
      </article>
    </div>
  </section>
</template>

<style scoped>
.runtime-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.surface-card {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(234, 88, 12, 0.08), transparent 45%),
    var(--sqlforge-surface-2);
  box-shadow: none;
  padding: 24px;
}

.runtime-hero,
.runtime-grid,
.summary-card-grid {
  display: grid;
  gap: 20px;
}

.runtime-hero {
  grid-template-columns: minmax(0, 2fr) minmax(260px, 1fr);
  align-items: start;
}

.summary-card-grid {
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.runtime-grid {
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
}

.runtime-eyebrow,
.section-kicker {
  margin: 0 0 12px;
  font-size: 12px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #c2410c;
}

.runtime-title,
.section-title {
  margin: 0;
  font-size: 28px;
  color: var(--sqlforge-text-primary);
}

.section-title {
  font-size: 22px;
}

.runtime-summary,
.runtime-note,
.section-summary {
  margin: 12px 0 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.65;
}

.table-wrap {
  overflow-x: auto;
}

.objective-table {
  width: 100%;
  border-collapse: collapse;
}

.objective-table th,
.objective-table td {
  padding: 12px 10px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.24);
  text-align: left;
  color: var(--sqlforge-text-secondary);
}

.objective-table th {
  color: #0f172a;
}

.bullet-list {
  margin: 0;
  padding-left: 18px;
  color: #334155;
  line-height: 1.7;
}

@media (max-width: 960px) {
  .runtime-hero {
    grid-template-columns: 1fr;
  }
}
</style>
