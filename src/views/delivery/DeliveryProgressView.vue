<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { createDeliveryProgressSnapshot } from './progressSnapshot'
import { deliveryProgressAvailability } from '../../config/runtimeFlags'

const { t, locale } = useI18n()
const snapshot = createDeliveryProgressSnapshot()

const summaryCards = computed(() => [
  {
    key: 'todo',
    value: snapshot.statusCounts.todo,
    tone: 'neutral'
  },
  {
    key: 'inProgress',
    value: snapshot.statusCounts.in_progress,
    tone: 'warning'
  },
  {
    key: 'inReview',
    value: snapshot.statusCounts.in_review,
    tone: 'success'
  },
  {
    key: 'blocked',
    value: snapshot.statusCounts.blocked,
    tone: 'danger'
  },
  {
    key: 'done',
    value: snapshot.statusCounts.done,
    tone: 'success'
  }
])

const sourceFiles = computed(() => snapshot.sourceFiles)
const activeTasks = computed(() => snapshot.activeTasks)
const completedTasks = computed(() => snapshot.completedTasks.slice(0, 6))
const moduleProgress = computed(() => snapshot.moduleProgress)
const recentChanges = computed(() => snapshot.recentChanges)
const blockerItems = computed(() => snapshot.blockerItems)
const dependencyChains = computed(() => snapshot.dependencyChains)
const validationEntries = computed(() => snapshot.validationEntries.slice(-6).reverse())
const hasExplicitBlocked = computed(() => snapshot.hasExplicitBlocked)
const runtimeAvailability = computed(() => deliveryProgressAvailability)
const runtimePills = computed(() => [
  {
    key: 'mode',
    label: t('deliveryProgress.runtime.modePill', {
      mode: runtimeAvailability.value.mode
    })
  },
  {
    key: 'flag',
    label: t(`deliveryProgress.runtime.flag.${runtimeAvailability.value.flagState}`)
  },
  {
    key: 'scope',
    label: t('deliveryProgress.runtime.scope')
  }
])

const statusTypeMap = {
  todo: '',
  in_progress: 'warning',
  in_review: 'success',
  blocked: 'danger',
  done: 'success',
  planned: 'info',
  untracked: 'info'
}

const changeKindTypeMap = {
  progress: 'warning',
  done: 'success',
  validation: 'info'
}

const formatProgressLog = progressLog => {
  if (!progressLog.length) {
    return locale.value === 'zh-CN' ? '暂无进度日志' : 'No progress log yet'
  }
  return progressLog[progressLog.length - 1]
}
</script>

<template>
  <section class="delivery-progress-page">
    <section class="delivery-hero">
      <div class="delivery-hero-copy">
        <p class="hero-eyebrow sqlforge-code-label">{{ t('deliveryProgress.eyebrow') }}</p>
        <h1 class="hero-title">{{ t('deliveryProgress.heroTitle') }}</h1>
        <p class="hero-summary">{{ t('deliveryProgress.heroSummary') }}</p>
        <div class="hero-pills">
          <span
            v-for="pill in runtimePills"
            :key="pill.key"
            class="hero-pill hero-pill-strong"
          >
            {{ pill.label }}
          </span>
          <span
            v-for="sourceFile in sourceFiles"
            :key="sourceFile"
            class="hero-pill"
          >
            {{ sourceFile }}
          </span>
        </div>
      </div>

      <div class="delivery-hero-card">
        <p class="delivery-hero-label sqlforge-code-label">{{ t('deliveryProgress.visibilityLabel') }}</p>
        <h2>{{ t('deliveryProgress.visibilityTitle') }}</h2>
        <p>{{ t(runtimeAvailability.visibilityReasonKey) }}</p>
        <div class="hero-card-list">
          <div class="hero-card-row">
            <span>{{ t('deliveryProgress.runtime.modeLabel') }}</span>
            <strong>{{ runtimeAvailability.mode }}</strong>
          </div>
          <div class="hero-card-row">
            <span>{{ t('deliveryProgress.runtime.flagLabel') }}</span>
            <strong>{{ t(`deliveryProgress.runtime.flag.${runtimeAvailability.flagState}`) }}</strong>
          </div>
          <div class="hero-card-row">
            <span>{{ t('deliveryProgress.runtime.scopeLabel') }}</span>
            <strong>{{ t('deliveryProgress.runtime.scope') }}</strong>
          </div>
        </div>
        <p class="hero-card-footnote">{{ t('deliveryProgress.visibilitySummary') }}</p>
      </div>
    </section>

    <section class="delivery-section">
      <div class="section-heading">
        <div>
          <p class="section-kicker sqlforge-code-label">{{ t('deliveryProgress.summaryTitle') }}</p>
          <h2 class="sqlforge-section-title">{{ t('deliveryProgress.summaryTitle') }}</h2>
        </div>
        <p class="section-summary">{{ t('deliveryProgress.summaryDescription') }}</p>
      </div>

      <div class="summary-grid">
        <article
          v-for="card in summaryCards"
          :key="card.key"
          class="summary-card"
          :class="`summary-card-${card.tone}`"
        >
          <p class="summary-card-label">{{ t(`deliveryProgress.cards.${card.key}`) }}</p>
          <p class="summary-card-value">{{ card.value }}</p>
        </article>
      </div>
    </section>

    <section class="delivery-grid">
      <section class="delivery-section">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">{{ t('deliveryProgress.activeTasksTitle') }}</p>
            <h2 class="sqlforge-section-title">{{ t('deliveryProgress.activeTasksTitle') }}</h2>
          </div>
          <p class="section-summary">{{ t('deliveryProgress.activeTasksDescription') }}</p>
        </div>

        <div class="task-list">
          <article
            v-for="task in activeTasks"
            :key="task.taskId"
            class="task-card"
          >
            <div class="task-card-header">
              <div>
                <p class="task-card-id sqlforge-code-label">{{ task.taskId }}</p>
                <h3 class="task-card-title">{{ task.name }}</h3>
              </div>
              <el-tag :type="statusTypeMap[task.status]">
                {{ t(`deliveryProgress.status.${task.statusKey}`) }}
              </el-tag>
            </div>

            <p class="task-card-scope">{{ task.scope }}</p>

            <div class="task-card-meta">
              <span>{{ t('deliveryProgress.meta.priority') }}: {{ task.priority }}</span>
              <span>{{ t('deliveryProgress.meta.dependsOn') }}: {{ task.dependsOn }}</span>
            </div>

            <p class="task-card-log">{{ formatProgressLog(task.progressLog) }}</p>
          </article>
        </div>
      </section>

      <section class="delivery-section">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">{{ t('deliveryProgress.moduleTitle') }}</p>
            <h2 class="sqlforge-section-title">{{ t('deliveryProgress.moduleTitle') }}</h2>
          </div>
          <p class="section-summary">{{ t('deliveryProgress.moduleDescription') }}</p>
        </div>

        <div class="module-list">
          <article
            v-for="module in moduleProgress"
            :key="module.moduleLabel"
            class="module-card"
          >
            <div class="module-card-header">
              <div>
                <p class="module-card-label sqlforge-code-label">{{ module.moduleLabel }}</p>
                <h3>{{ module.completionRate }}%</h3>
              </div>
              <p class="module-card-total">{{ module.total }} {{ t('deliveryProgress.meta.totalTasks') }}</p>
            </div>

            <div class="module-card-bar">
              <div
                class="module-card-bar-fill"
                :style="{ width: `${module.completionRate}%` }"
              />
            </div>

            <div class="module-card-stats">
              <span>{{ t('deliveryProgress.status.done') }} {{ module.done }}</span>
              <span>{{ t('deliveryProgress.status.in_progress') }} {{ module.in_progress }}</span>
              <span>{{ t('deliveryProgress.status.in_review') }} {{ module.in_review }}</span>
              <span>{{ t('deliveryProgress.status.todo') }} {{ module.todo }}</span>
              <span>{{ t('deliveryProgress.status.blocked') }} {{ module.blocked }}</span>
            </div>
          </article>
        </div>
      </section>
    </section>

    <section class="delivery-grid">
      <section class="delivery-section">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">{{ t('deliveryProgress.recentChangesTitle') }}</p>
            <h2 class="sqlforge-section-title">{{ t('deliveryProgress.recentChangesTitle') }}</h2>
          </div>
          <p class="section-summary">{{ t('deliveryProgress.recentChangesDescription') }}</p>
        </div>

        <div class="timeline-list">
          <article
            v-for="item in recentChanges"
            :key="item.itemKey"
            class="timeline-card"
          >
            <div class="timeline-card-header">
              <div>
                <p class="timeline-card-id sqlforge-code-label">{{ item.taskId }}</p>
                <h3>{{ item.title }}</h3>
              </div>
              <el-tag :type="changeKindTypeMap[item.kind]">
                {{ t(`deliveryProgress.changeKind.${item.kind}`) }}
              </el-tag>
            </div>
            <p class="timeline-card-line">{{ item.detail }}</p>
            <div class="timeline-card-meta">
              <span>{{ item.timestampLabel }}</span>
              <span>{{ item.sourceFile }}</span>
            </div>
            <p
              v-if="item.auxiliary"
              class="timeline-card-line timeline-card-line-muted"
            >
              {{ item.auxiliary }}
            </p>
          </article>
        </div>
      </section>

      <section class="delivery-section">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">{{ t('deliveryProgress.blockedTitle') }}</p>
            <h2 class="sqlforge-section-title">{{ t('deliveryProgress.blockedTitle') }}</h2>
          </div>
          <p class="section-summary">{{ t('deliveryProgress.blockedDescription') }}</p>
        </div>

        <div
          v-if="blockerItems.length"
          class="task-list"
        >
          <article
            v-for="item in blockerItems"
            :key="item.taskId"
            class="task-card task-card-danger"
          >
            <div class="task-card-header">
              <div>
                <p class="task-card-id sqlforge-code-label">{{ item.taskId }}</p>
                <h3 class="task-card-title">{{ item.name }}</h3>
              </div>
              <el-tag :type="statusTypeMap[item.status]">
                {{ t(`deliveryProgress.status.${item.status}`) }}
              </el-tag>
            </div>

            <p class="task-card-log">{{ item.reason }}</p>
            <div class="task-card-meta">
              <span>{{ t('deliveryProgress.meta.dependsOn') }}: {{ item.dependsOn }}</span>
              <span>{{ t('deliveryProgress.meta.blockedSource') }}</span>
            </div>
          </article>
        </div>

        <article
          v-else
          class="empty-card"
        >
          <h3>{{ t('deliveryProgress.empty.blockedTitle') }}</h3>
          <p>{{ t('deliveryProgress.empty.blockedDescription') }}</p>
        </article>

        <p class="section-footnote">
          {{
            hasExplicitBlocked
              ? t('deliveryProgress.blockedFootnote')
              : t('deliveryProgress.pendingFootnote')
          }}
        </p>
      </section>
    </section>

    <section class="delivery-section">
      <div class="section-heading">
        <div>
          <p class="section-kicker sqlforge-code-label">{{ t('deliveryProgress.dependencyTitle') }}</p>
          <h2 class="sqlforge-section-title">{{ t('deliveryProgress.dependencyTitle') }}</h2>
        </div>
        <p class="section-summary">{{ t('deliveryProgress.dependencyDescription') }}</p>
      </div>

      <div
        v-if="dependencyChains.length"
        class="dependency-list"
      >
        <article
          v-for="chain in dependencyChains"
          :key="chain.taskId"
          class="dependency-card"
        >
          <div class="dependency-card-header">
            <div>
              <p class="timeline-card-id sqlforge-code-label">{{ chain.taskId }}</p>
              <h3>{{ chain.name }}</h3>
            </div>
            <div class="dependency-card-tags">
              <el-tag :type="statusTypeMap[chain.status]">
                {{ t(`deliveryProgress.status.${chain.statusKey}`) }}
              </el-tag>
              <span class="dependency-count">
                {{ t('deliveryProgress.meta.unresolvedCount', { count: chain.unresolvedCount }) }}
              </span>
            </div>
          </div>

          <div class="dependency-chip-list">
            <article
              v-for="dependency in chain.dependencies"
              :key="`${chain.taskId}-${dependency.dependencyId}`"
              class="dependency-chip"
            >
              <div class="dependency-chip-header">
                <strong>{{ dependency.dependencyId }}</strong>
                <el-tag size="small" :type="statusTypeMap[dependency.status]">
                  {{ t(`deliveryProgress.status.${dependency.statusKey}`) }}
                </el-tag>
              </div>
              <p>{{ dependency.label }}</p>
              <span>{{ t(`deliveryProgress.dependencySource.${dependency.source}`) }}</span>
            </article>
          </div>
        </article>
      </div>

      <article
        v-else
        class="empty-card"
      >
        <h3>{{ t('deliveryProgress.empty.dependencyTitle') }}</h3>
        <p>{{ t('deliveryProgress.empty.dependencyDescription') }}</p>
      </article>
    </section>

    <section class="delivery-grid">
      <section class="delivery-section">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">{{ t('deliveryProgress.completedTitle') }}</p>
            <h2 class="sqlforge-section-title">{{ t('deliveryProgress.completedTitle') }}</h2>
          </div>
          <p class="section-summary">{{ t('deliveryProgress.completedDescription') }}</p>
        </div>

        <div class="timeline-list">
          <article
            v-for="task in completedTasks"
            :key="task.taskId"
            class="timeline-card"
          >
            <div class="timeline-card-header">
              <div>
                <p class="timeline-card-id sqlforge-code-label">{{ task.taskId }}</p>
                <h3>{{ task.name }}</h3>
              </div>
              <span class="timeline-card-time">{{ task.completedAt }}</span>
            </div>
            <p class="timeline-card-line">{{ task.commitSubject }}</p>
          </article>
        </div>
      </section>

      <section class="delivery-section">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">{{ t('deliveryProgress.validationTitle') }}</p>
            <h2 class="sqlforge-section-title">{{ t('deliveryProgress.validationTitle') }}</h2>
          </div>
          <p class="section-summary">{{ t('deliveryProgress.validationDescription') }}</p>
        </div>

        <div class="timeline-list">
          <article
            v-for="entry in validationEntries"
            :key="`${entry.timestamp}-${entry.trigger}`"
            class="timeline-card"
          >
            <div class="timeline-card-header">
              <div>
                <p class="timeline-card-id sqlforge-code-label">{{ entry.trigger }}</p>
                <h3>{{ entry.result }}</h3>
              </div>
              <span class="timeline-card-time">{{ entry.timestamp }}</span>
            </div>
            <p class="timeline-card-line">{{ entry.rules }}</p>
            <p class="timeline-card-line timeline-card-line-muted">{{ entry.evidence }}</p>
          </article>
        </div>
      </section>
    </section>
  </section>
</template>

<style scoped>
.delivery-progress-page {
  display: grid;
  gap: 24px;
}

.delivery-hero,
.delivery-section {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-xl);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.03), transparent 36%),
    var(--sqlforge-surface-3);
}

.delivery-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.8fr) minmax(320px, 0.9fr);
  gap: 24px;
  padding: 28px;
}

.hero-eyebrow,
.section-kicker,
.delivery-hero-label,
.task-card-id,
.module-card-label,
.timeline-card-id {
  margin: 0;
  color: var(--sqlforge-text-muted);
}

.hero-title {
  margin: 10px 0 0;
  font-size: 38px;
  font-weight: 400;
  line-height: 1.05;
}

.hero-summary {
  margin: 12px 0 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.7;
}

.hero-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.hero-pill {
  display: inline-flex;
  align-items: center;
  padding: 7px 12px;
  border: 1px solid var(--sqlforge-border-strong);
  border-radius: var(--sqlforge-radius-pill);
  background: rgba(255, 255, 255, 0.03);
  color: var(--sqlforge-text-secondary);
  font-size: 12px;
}

.hero-pill-strong {
  border-color: var(--sqlforge-color-brand-border);
  background: rgba(62, 207, 142, 0.08);
  color: var(--sqlforge-text-primary);
}

.delivery-hero-card {
  padding: 20px;
  border: 1px solid var(--sqlforge-color-brand-border);
  border-radius: var(--sqlforge-radius-lg);
  background: rgba(62, 207, 142, 0.08);
}

.delivery-hero-card h2 {
  margin: 10px 0 0;
  font-size: 24px;
  font-weight: 400;
}

.delivery-hero-card p:last-child {
  margin: 12px 0 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}

.hero-card-list {
  display: grid;
  gap: 12px;
  margin-top: 18px;
}

.hero-card-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  color: var(--sqlforge-text-secondary);
}

.hero-card-row strong {
  color: var(--sqlforge-text-primary);
  font-weight: 500;
}

.hero-card-footnote {
  margin: 16px 0 0;
}

.delivery-section {
  padding: 24px;
}

.delivery-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 24px;
}

.section-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.section-summary {
  max-width: 380px;
  margin: 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
  text-align: right;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
}

.summary-card,
.task-card,
.module-card,
.timeline-card,
.dependency-card,
.empty-card {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-lg);
  background: var(--sqlforge-surface-2);
}

.summary-card {
  padding: 18px;
}

.summary-card-label {
  margin: 0;
  color: var(--sqlforge-text-muted);
  font-size: 13px;
}

.summary-card-value {
  margin: 12px 0 0;
  font-size: 30px;
  font-weight: 400;
}

.summary-card-warning {
  border-color: rgba(214, 179, 48, 0.28);
}

.summary-card-danger {
  border-color: rgba(236, 106, 94, 0.28);
}

.summary-card-success {
  border-color: rgba(62, 207, 142, 0.28);
}

.task-list,
.module-list,
.timeline-list,
.dependency-list {
  display: grid;
  gap: 14px;
}

.task-card,
.module-card,
.timeline-card,
.dependency-card,
.empty-card {
  padding: 18px;
}

.task-card-header,
.module-card-header,
.timeline-card-header,
.dependency-card-header,
.dependency-chip-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.task-card-title,
.module-card h3,
.timeline-card h3 {
  margin: 8px 0 0;
  font-size: 18px;
  font-weight: 500;
}

.task-card-scope,
.task-card-log,
.timeline-card-line {
  margin: 12px 0 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}

.task-card-meta,
.module-card-stats,
.timeline-card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
  margin-top: 12px;
  color: var(--sqlforge-text-muted);
  font-size: 13px;
}

.module-card-total,
.timeline-card-time {
  color: var(--sqlforge-text-muted);
  font-size: 12px;
}

.module-card-bar {
  height: 10px;
  margin-top: 14px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--sqlforge-bg-page-deep);
}

.module-card-bar-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, rgba(62, 207, 142, 0.48), rgba(62, 207, 142, 0.9));
}

.timeline-card-line-muted {
  color: var(--sqlforge-text-muted);
}

.task-card-danger {
  border-color: rgba(236, 106, 94, 0.28);
}

.section-footnote {
  margin: 14px 0 0;
  color: var(--sqlforge-text-muted);
  font-size: 13px;
}

.dependency-card-tags,
.dependency-chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.dependency-count {
  color: var(--sqlforge-text-muted);
  font-size: 12px;
}

.dependency-chip {
  min-width: 220px;
  padding: 14px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-md);
  background: rgba(255, 255, 255, 0.02);
}

.dependency-chip p,
.empty-card p {
  margin: 10px 0 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}

.dependency-chip span {
  display: inline-flex;
  margin-top: 10px;
  color: var(--sqlforge-text-muted);
  font-size: 12px;
}

.empty-card h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
}

@media (max-width: 1280px) {
  .summary-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1080px) {
  .delivery-hero,
  .delivery-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .section-heading {
    flex-direction: column;
    align-items: flex-start;
  }

  .section-summary {
    max-width: none;
    text-align: left;
  }
}

@media (max-width: 720px) {
  .delivery-hero,
  .delivery-section {
    padding: 20px;
  }

  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .task-card-header,
  .module-card-header,
  .timeline-card-header,
  .dependency-card-header,
  .dependency-chip-header {
    flex-direction: column;
  }
}
</style>
