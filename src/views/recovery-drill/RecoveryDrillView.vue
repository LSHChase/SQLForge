<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import EvidencePanel from '../common/EvidencePanel.vue'
import PageHero from '../common/PageHero.vue'

const { t, locale } = useI18n()
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
    <PageHero
      v-bind="{
        eyebrow: t('recoveryDrill.heroEyebrow'),
        title: t('recoveryDrill.heroTitle'),
        summary: t('recoveryDrill.heroSummary')
      }"
    >
      <template #aside>
        <EvidencePanel tone="warning">
          <p class="runtime-note">{{ t('recoveryDrill.heroNote') }}</p>
        </EvidencePanel>
      </template>
    </PageHero>

    <div class="summary-card-grid">
      <EvidencePanel
        v-for="item in inventoryItems"
        :key="item.title"
        as="article"
        v-bind="{
          eyebrow: item.title,
          title: item.title,
          summary: item.summary
        }"
      />
    </div>

    <div class="runtime-grid">
      <EvidencePanel
        as="article"
        v-bind="{
          eyebrow: t('recoveryDrill.objectivesEyebrow'),
          title: t('recoveryDrill.objectivesTitle')
        }"
      >
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
      </EvidencePanel>

      <EvidencePanel
        as="article"
        v-bind="{
          eyebrow: t('recoveryDrill.checklistEyebrow'),
          title: t('recoveryDrill.checklistTitle')
        }"
        tone="warning"
      >
        <ul class="bullet-list">
          <li
            v-for="item in checklistItems"
            :key="item"
          >
            {{ item }}
          </li>
        </ul>
      </EvidencePanel>
    </div>
  </section>
</template>

<style scoped>
.runtime-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.runtime-grid,
.summary-card-grid {
  display: grid;
  gap: 20px;
}

.summary-card-grid {
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.runtime-grid {
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
}

.runtime-note {
  margin: 0;
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
  color: var(--sqlforge-text-primary);
}

.bullet-list {
  margin: 0;
  padding-left: 18px;
  color: var(--sqlforge-text-secondary);
  line-height: 1.7;
}
</style>
