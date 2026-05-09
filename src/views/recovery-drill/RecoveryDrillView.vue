<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import EvidencePanel from '../common/EvidencePanel.vue'
import MetricCard from '../common/MetricCard.vue'
import PageHero from '../common/PageHero.vue'
import SectionHeader from '../common/SectionHeader.vue'

const { t } = useI18n()

const inventoryItems = computed(() => [
  {
    key: 'core',
    title: 'MySQL / Core Traceability',
    summary: t('recoveryDrill.inventory.core')
  },
  {
    key: 'audit',
    title: 'audit_log',
    summary: t('recoveryDrill.inventory.audit')
  },
  {
    key: 'export',
    title: 'export_record / archive pointers',
    summary: t('recoveryDrill.inventory.export')
  },
  {
    key: 'queue',
    title: 'kafka_message_queue',
    summary: t('recoveryDrill.inventory.queue')
  },
  {
    key: 'keys',
    title: 'system_config / key boundary',
    summary: t('recoveryDrill.inventory.keys')
  }
])

const objectiveRows = computed(() => [
  ['governance / core metadata', '< 1h', '< 4h', 'DBA / Platform Ops'],
  ['governance / audit_log', '< 1h', '< 4h', 'DBA / Compliance Ops'],
  ['governance / export_record', '< 1h', '< 4h', 'DBA / Storage Ops'],
  ['governance / kafka_message_queue', '< 1h', '< 4h', 'DBA / Messaging Ops'],
  ['governance / system_config + keys', t('recoveryDrill.sameBackupBatch'), t('recoveryDrill.sameBackupBatch'), 'Security Ops']
])

const checklistItems = computed(() => [
  t('recoveryDrill.checklist.health'),
  t('recoveryDrill.checklist.audit'),
  t('recoveryDrill.checklist.backlog'),
  t('recoveryDrill.checklist.export'),
  t('recoveryDrill.checklist.leak')
])

const recoveryMetrics = computed(() => [
  {
    key: 'inventory',
    label: t('recoveryDrill.metrics.inventory.label'),
    value: inventoryItems.value.length,
    trend: t('recoveryDrill.metrics.inventory.trend'),
    detail: t('recoveryDrill.metrics.inventory.detail'),
    tone: 'neutral'
  },
  {
    key: 'rpo',
    label: t('recoveryDrill.metrics.rpo.label'),
    value: '< 1h',
    trend: 'RPO',
    detail: t('recoveryDrill.metrics.rpo.detail'),
    tone: 'success'
  },
  {
    key: 'rto',
    label: t('recoveryDrill.metrics.rto.label'),
    value: '< 4h',
    trend: 'RTO',
    detail: t('recoveryDrill.metrics.rto.detail'),
    tone: 'success'
  },
  {
    key: 'checklist',
    label: t('recoveryDrill.metrics.checklist.label'),
    value: checklistItems.value.length,
    trend: t('recoveryDrill.metrics.checklist.trend'),
    detail: t('recoveryDrill.metrics.checklist.detail'),
    tone: 'warning'
  }
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

    <section class="runtime-section">
      <SectionHeader
        eyebrow="kpi"
        :title="t('recoveryDrill.kpiTitle')"
        :summary="t('recoveryDrill.kpiSummary')"
      />
      <div class="metric-grid">
        <MetricCard
          v-for="metric in recoveryMetrics"
          :key="metric.key"
          v-bind="{
            label: metric.label,
            value: metric.value,
            trend: metric.trend,
            detail: metric.detail,
            tone: metric.tone
          }"
        />
      </div>
    </section>

    <div class="runtime-grid">
      <EvidencePanel
        as="article"
        :eyebrow="t('recoveryDrill.activityEyebrow')"
        :title="t('recoveryDrill.activityTitle')"
        :summary="t('recoveryDrill.activitySummary')"
      >
        <div class="activity-list">
          <article
            v-for="item in inventoryItems"
            :key="item.key"
            class="activity-item"
          >
            <p class="activity-type sqlforge-code-label">{{ item.title }}</p>
            <h3>{{ item.title }}</h3>
            <p>{{ item.summary }}</p>
          </article>
        </div>
      </EvidencePanel>

      <EvidencePanel
        as="article"
        :eyebrow="t('recoveryDrill.checklistEyebrow')"
        :title="t('recoveryDrill.checklistTitle')"
        :summary="t('recoveryDrill.checklistSummary')"
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

    <EvidencePanel
      :eyebrow="t('recoveryDrill.objectivesEyebrow')"
      :title="t('recoveryDrill.objectivesTitle')"
      :summary="t('recoveryDrill.objectivesSummary')"
    >
      <div class="table-wrap">
        <table class="objective-table">
          <thead>
            <tr>
              <th>{{ t('recoveryDrill.table.domain') }}</th>
              <th>RPO</th>
              <th>RTO</th>
              <th>{{ t('recoveryDrill.table.owner') }}</th>
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
  </section>
</template>

<style scoped>
.runtime-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.runtime-grid,
.runtime-section,
.metric-grid,
.activity-list {
  display: grid;
  gap: var(--sqlforge-space-5);
}

.metric-grid {
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

.activity-item {
  display: grid;
  gap: var(--sqlforge-space-2);
  padding: var(--sqlforge-space-5);
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-lg);
  background: var(--sqlforge-surface-2);
}

.activity-item h3,
.activity-item p {
  margin: 0;
}

.activity-item h3 {
  color: var(--sqlforge-text-primary);
  font-size: var(--sqlforge-text-heading);
  font-weight: 500;
}

.activity-item p {
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}

.activity-type {
  color: var(--sqlforge-text-muted);
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
