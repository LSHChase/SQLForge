<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import EvidencePanel from '../common/EvidencePanel.vue'
import MetricCard from '../common/MetricCard.vue'
import PageHero from '../common/PageHero.vue'
import SectionHeader from '../common/SectionHeader.vue'

const { t } = useI18n()

const gateCards = computed(() => [
  {
    key: 'entry',
    title: t('runtimeGates.gates.entry.title'),
    summary: t('runtimeGates.gates.entry.summary'),
    checks: [
      'python3 scripts/task_audit.py --check',
      'python3 scripts/foreman.py compile-governance --check',
      'node scripts/lint-repository-knowledge.js'
    ]
  },
  {
    key: 'delivery',
    title: t('runtimeGates.gates.delivery.title'),
    summary: t('runtimeGates.gates.delivery.summary'),
    checks: [
      'bash scripts/verify-db-scripts.sh',
      'mvn -B clean install',
      'bash scripts/run-coverage.sh --phase phase0|phase1plus',
      'bash scripts/run-sonar.sh --require-config',
      'npm run lint && npm run build'
    ]
  },
  {
    key: 'compliance',
    title: t('runtimeGates.gates.compliance.title'),
    summary: t('runtimeGates.gates.compliance.summary'),
    checks: [
      'python3 scripts/verify_compliance_baseline.py',
      'python3 scripts/verify_kafka_runtime_config.py',
      'bash scripts/run-kafka-runtime-gate.sh'
    ]
  }
])

const blockerItems = computed(() => [
  t('runtimeGates.blockers.coverage'),
  t('runtimeGates.blockers.sonar'),
  t('runtimeGates.blockers.workflowDispatch')
])

const evidenceRows = computed(() => [
  {
    label: t('runtimeGates.evidenceRows.defaultCi'),
    value: 'compose config + DB script gate + runtime smoke'
  },
  {
    label: t('runtimeGates.evidenceRows.kafka'),
    value: '.github/workflows/kafka-runtime-gate.yml'
  },
  {
    label: t('runtimeGates.evidenceRows.phaseGate'),
    value: '.github/workflows/phase-gate.yml'
  },
  {
    label: t('runtimeGates.evidenceRows.phaseScript'),
    value: 'scripts/run-phase-gates.sh'
  }
])

const runtimeMetrics = computed(() => {
  const checkCount = gateCards.value.reduce((total, gate) => total + gate.checks.length, 0)

  return [
    {
      key: 'gate-count',
      label: t('runtimeGates.metrics.gates.label'),
      value: gateCards.value.length,
      trend: 'Entry / Delivery / Compliance',
      detail: t('runtimeGates.metrics.gates.detail'),
      tone: 'success'
    },
    {
      key: 'check-count',
      label: t('runtimeGates.metrics.checks.label'),
      value: checkCount,
      trend: t('runtimeGates.metrics.checks.trend'),
      detail: t('runtimeGates.metrics.checks.detail'),
      tone: 'neutral'
    },
    {
      key: 'workflow-count',
      label: t('runtimeGates.metrics.workflows.label'),
      value: evidenceRows.value.filter(row => row.value.includes('.github/workflows')).length,
      trend: '.github/workflows',
      detail: t('runtimeGates.metrics.workflows.detail'),
      tone: 'neutral'
    },
    {
      key: 'blocker-count',
      label: t('runtimeGates.metrics.blockers.label'),
      value: blockerItems.value.length,
      trend: t('runtimeGates.metrics.blockers.trend'),
      detail: t('runtimeGates.metrics.blockers.detail'),
      tone: 'warning'
    }
  ]
})
</script>

<template>
  <section class="runtime-page" data-testid="runtime-gates-page">
    <PageHero
      v-bind="{
        eyebrow: t('runtimeGates.heroEyebrow'),
        title: t('runtimeGates.heroTitle'),
        summary: t('runtimeGates.heroSummary')
      }"
    >
      <template #aside>
        <EvidencePanel tone="warning">
          <p class="runtime-note">{{ t('runtimeGates.heroNote') }}</p>
        </EvidencePanel>
      </template>
    </PageHero>

    <section class="runtime-section">
      <SectionHeader
        eyebrow="kpi"
        :title="t('runtimeGates.kpiTitle')"
        :summary="t('runtimeGates.kpiSummary')"
      />
      <div class="metric-grid">
        <MetricCard
          v-for="metric in runtimeMetrics"
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
        :eyebrow="t('runtimeGates.activityEyebrow')"
        :title="t('runtimeGates.activityTitle')"
        :summary="t('runtimeGates.activitySummary')"
      >
        <div class="activity-list">
          <article
            v-for="gate in gateCards"
            :key="gate.key"
            class="activity-item"
          >
            <div class="activity-copy">
              <p class="activity-type sqlforge-code-label">{{ gate.title }}</p>
              <h3>{{ gate.title }}</h3>
              <p>{{ gate.summary }}</p>
            </div>
            <div class="check-list">
              <span
                v-for="item in gate.checks"
                :key="item"
                class="check-pill"
              >
                {{ item }}
              </span>
            </div>
          </article>
        </div>
      </EvidencePanel>

      <EvidencePanel
        as="article"
        :eyebrow="t('runtimeGates.blockersEyebrow')"
        :title="t('runtimeGates.blockersTitle')"
        :summary="t('runtimeGates.blockersSummary')"
        tone="warning"
      >
        <ul class="bullet-list">
          <li
            v-for="item in blockerItems"
            :key="item"
          >
            {{ item }}
          </li>
        </ul>
      </EvidencePanel>
    </div>

    <EvidencePanel
      :eyebrow="t('runtimeGates.evidenceEyebrow')"
      :title="t('runtimeGates.evidenceTitle')"
      :summary="t('runtimeGates.evidenceSummary')"
    >
      <div class="evidence-grid">
        <div
          v-for="row in evidenceRows"
          :key="row.label"
          class="evidence-item"
        >
          <span class="evidence-label">{{ row.label }}</span>
          <strong>{{ row.value }}</strong>
        </div>
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

.check-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.check-pill {
  border-radius: 999px;
  padding: 8px 12px;
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-primary);
  font-size: 13px;
}

.activity-item {
  display: grid;
  gap: var(--sqlforge-space-4);
  padding: var(--sqlforge-space-5);
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-lg);
  background: var(--sqlforge-surface-2);
}

.activity-copy {
  display: grid;
  gap: var(--sqlforge-space-2);
}

.activity-copy h3,
.activity-copy p {
  margin: 0;
}

.activity-copy h3 {
  color: var(--sqlforge-text-primary);
  font-size: var(--sqlforge-text-heading);
  font-weight: 500;
}

.activity-copy p {
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}

.activity-type {
  color: var(--sqlforge-text-muted);
}

.evidence-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

.evidence-item {
  border-radius: 18px;
  padding: 18px;
  background: rgba(35, 35, 35, 0.92);
  border: 1px solid var(--sqlforge-border-default);
}

.evidence-label {
  display: block;
  margin-bottom: 8px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--sqlforge-text-muted);
}

.bullet-list {
  margin: 0;
  padding-left: 18px;
  color: var(--sqlforge-text-secondary);
  line-height: 1.7;
}
</style>
