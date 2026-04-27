<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const { locale } = useI18n()
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
    <div class="runtime-hero surface-card">
      <div>
        <p class="runtime-eyebrow sqlforge-code-label">phase-f runtime gates</p>
        <h1 class="runtime-title">
          {{ isChinese ? '运行时门禁与退出阻断基线' : 'Runtime Gates And Exit Blocking Baseline' }}
        </h1>
        <p class="runtime-summary">
          {{
            isChinese
              ? '这里收口 Entry / Delivery / Compliance 三层门禁，避免 Phase-F 的 build、runtime、恢复和合规证据继续散在脚本与文档里。'
              : 'This page consolidates the Entry, Delivery, and Compliance gates so Phase-F build, runtime, recovery, and compliance evidence no longer drift across scripts and docs.'
          }}
        </p>
      </div>
      <p class="runtime-note">
        {{
          isChinese
            ? '当前重点不是再加展示页，而是明确哪些脚本已经成为阻断门禁、哪些仍是残余风险。'
            : 'The point is not more display pages, but a clear line between blocking gates and residual risks.'
        }}
      </p>
    </div>

    <div class="summary-card-grid">
      <article
        v-for="gate in gateCards"
        :key="gate.key"
        class="surface-card gate-card"
      >
        <p class="section-kicker sqlforge-code-label">{{ gate.title }}</p>
        <h2 class="section-title">{{ gate.title }}</h2>
        <p class="section-summary">{{ gate.summary }}</p>
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

    <div class="runtime-grid">
      <article class="surface-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">evidence map</p>
            <h2 class="section-title">{{ isChinese ? '脚本与 workflow 入口' : 'Scripts And Workflow Entry Points' }}</h2>
          </div>
        </div>

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
      </article>

      <article class="surface-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">residual blockers</p>
            <h2 class="section-title">{{ isChinese ? '仍需继续推进的阻塞项' : 'Residual Blockers Still Open' }}</h2>
          </div>
        </div>

        <ul class="bullet-list">
          <li
            v-for="item in blockerItems"
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
    radial-gradient(circle at top right, rgba(14, 165, 233, 0.08), transparent 45%),
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
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
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
  color: #0f766e;
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
  color: #0f766e;
}

.bullet-list {
  margin: 0;
  padding-left: 18px;
  color: var(--sqlforge-text-secondary);
  line-height: 1.7;
}

@media (max-width: 960px) {
  .runtime-hero {
    grid-template-columns: 1fr;
  }
}
</style>
