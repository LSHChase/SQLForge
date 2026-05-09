<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import EvidencePanel from '../common/EvidencePanel.vue'
import PageHero from '../common/PageHero.vue'

const { t, locale } = useI18n()
const isChinese = computed(() => locale.value === 'zh-CN')

const gateCards = computed(() => [
  {
    key: 'entry',
    title: isChinese.value ? 'Entry Gate' : 'Entry Gate',
    summary: isChinese.value
      ? '台账、治理编译物和仓库知识 lint 必须先对齐。'
      : 'Task ledger, compiled governance policy, and repository knowledge lint must pass first.',
    checks: [
      'python3 scripts/task_audit.py --check',
      'python3 scripts/foreman.py compile-governance --check',
      'node scripts/lint-repository-knowledge.js'
    ]
  },
  {
    key: 'delivery',
    title: isChinese.value ? 'Delivery Gate' : 'Delivery Gate',
    summary: isChinese.value
      ? '数据库脚本、构建、覆盖率和 Sonar 统一收口到阶段交付门禁。'
      : 'Database scripts, build, coverage, and Sonar are wired into the delivery gate.',
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
    title: isChinese.value ? 'Compliance Gate' : 'Compliance Gate',
    summary: isChinese.value
      ? '恢复基线、可观测基线、Kafka gate 与敏感数据边界纳入 R-118 复验。'
      : 'Recovery baseline, observability baseline, Kafka gate, and sensitive-data controls now feed R-118 rechecks.',
    checks: [
      'python3 scripts/verify_compliance_baseline.py',
      'python3 scripts/verify_kafka_runtime_config.py',
      'bash scripts/run-kafka-runtime-gate.sh'
    ]
  }
])

const blockerItems = computed(() => [
  isChinese.value
    ? 'Coverage threshold 已被 phase gate 真正执行，但当前仓库全量覆盖率仍需继续抬升到 Phase-1+ 85%。'
    : 'The phase gate now enforces coverage thresholds, but the repository still needs higher overall line coverage to reach the Phase-1+ 85% bar.',
  isChinese.value
    ? 'Sonar 在 delivery/full gate 下已被强制要求，缺少 secrets 时会直接阻断。'
    : 'Sonar is now mandatory in delivery/full gates and will block when secrets are missing.',
  isChinese.value
    ? 'Phase Gate workflow 仍然是显式 workflow_dispatch，不会自动绑定发布动作。'
    : 'The Phase Gate workflow remains an explicit workflow_dispatch gate instead of an automatically bound release action.'
])

const evidenceRows = computed(() => [
  {
    label: isChinese.value ? 'CI 默认门禁' : 'Default CI gate',
    value: 'compose config + DB script gate + runtime smoke'
  },
  {
    label: isChinese.value ? '真实 Kafka 门禁' : 'Real Kafka gate',
    value: '.github/workflows/kafka-runtime-gate.yml'
  },
  {
    label: isChinese.value ? 'Phase Gate 入口' : 'Phase Gate entrypoint',
    value: '.github/workflows/phase-gate.yml'
  },
  {
    label: isChinese.value ? '阶段脚本' : 'Phase script',
    value: 'scripts/run-phase-gates.sh'
  }
])
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

    <div class="summary-card-grid">
      <EvidencePanel
        v-for="gate in gateCards"
        :key="gate.key"
        as="article"
        v-bind="{
          eyebrow: gate.title,
          title: gate.title,
          summary: gate.summary
        }"
      >
        <div class="check-list">
          <span
            v-for="item in gate.checks"
            :key="item"
            class="check-pill"
          >
            {{ item }}
          </span>
        </div>
      </EvidencePanel>
    </div>

    <div class="runtime-grid">
      <EvidencePanel
        as="article"
        v-bind="{
          eyebrow: t('runtimeGates.evidenceEyebrow'),
          title: t('runtimeGates.evidenceTitle')
        }"
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

      <EvidencePanel
        as="article"
        v-bind="{
          eyebrow: t('runtimeGates.blockersEyebrow'),
          title: t('runtimeGates.blockersTitle')
        }"
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
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
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
  margin-top: 16px;
}

.check-pill {
  border-radius: 999px;
  padding: 8px 12px;
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-primary);
  font-size: 13px;
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
