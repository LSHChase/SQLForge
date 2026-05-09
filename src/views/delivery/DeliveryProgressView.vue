<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { createDeliveryProgressSnapshot } from './progressSnapshot'
import { deliveryProgressAvailability } from '../../config/runtimeFlags'
import EvidencePanel from '../common/EvidencePanel.vue'
import MetricCard from '../common/MetricCard.vue'
import PageHero from '../common/PageHero.vue'
import SectionHeader from '../common/SectionHeader.vue'

const { t } = useI18n()
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
const heroPills = computed(() => [
  ...runtimePills.value.map(pill => pill.label),
  ...sourceFiles.value
])
const visibilityRows = computed(() => [
  {
    key: 'mode',
    label: t('deliveryProgress.runtime.modeLabel'),
    value: runtimeAvailability.value.mode
  },
  {
    key: 'flag',
    label: t('deliveryProgress.runtime.flagLabel'),
    value: t(`deliveryProgress.runtime.flag.${runtimeAvailability.value.flagState}`)
  },
  {
    key: 'scope',
    label: t('deliveryProgress.runtime.scopeLabel'),
    value: t('deliveryProgress.runtime.scope')
  }
])
const summaryMetrics = computed(() =>
  summaryCards.value.map(card => ({
    ...card,
    label: t(`deliveryProgress.cards.${card.key}`),
    trend: card.key === 'done' ? 'tasks-done.md' : 'tasks.md'
  }))
)

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
    return t('deliveryProgress.empty.progressLog')
  }
  return progressLog[progressLog.length - 1]
}
</script>

<template>
  <section class="delivery-progress-page" data-testid="delivery-progress-page">
    <PageHero
      :eyebrow="t('deliveryProgress.eyebrow')"
      :title="t('deliveryProgress.heroTitle')"
      :summary="t('deliveryProgress.heroSummary')"
      :pills="heroPills"
    >
      <template #aside>
        <EvidencePanel
          tone="warning"
          :eyebrow="t('deliveryProgress.visibilityLabel')"
          :title="t('deliveryProgress.visibilityTitle')"
          :summary="t(runtimeAvailability.visibilityReasonKey)"
        >
          <div class="delivery-evidence-rows">
            <div
              v-for="row in visibilityRows"
              :key="row.key"
              class="delivery-evidence-row"
            >
              <span>{{ row.label }}</span>
              <strong>{{ row.value }}</strong>
            </div>
          </div>
          <template #footer>
            {{ t('deliveryProgress.visibilitySummary') }}
          </template>
        </EvidencePanel>
      </template>
    </PageHero>

    <section class="delivery-section">
      <SectionHeader
        eyebrow="kpi"
        :title="t('deliveryProgress.summaryTitle')"
        :summary="t('deliveryProgress.summaryDescription')"
      />

      <div class="summary-grid">
        <MetricCard
          v-for="card in summaryMetrics"
          :key="card.key"
          :label="card.label"
          :value="card.value"
          :trend="card.trend"
          :tone="card.tone"
        />
      </div>
    </section>

    <section class="delivery-grid">
      <EvidencePanel
        :eyebrow="t('deliveryProgress.activeTasksTitle')"
        :title="t('deliveryProgress.activeTasksTitle')"
        :summary="t('deliveryProgress.activeTasksDescription')"
      >
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
      </EvidencePanel>

      <EvidencePanel
        :eyebrow="t('deliveryProgress.moduleTitle')"
        :title="t('deliveryProgress.moduleTitle')"
        :summary="t('deliveryProgress.moduleDescription')"
      >
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
      </EvidencePanel>
    </section>

    <section class="delivery-grid">
      <EvidencePanel
        eyebrow="activity stream"
        :title="t('deliveryProgress.recentChangesTitle')"
        :summary="t('deliveryProgress.recentChangesDescription')"
      >
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
      </EvidencePanel>

      <EvidencePanel
        eyebrow="risk queue"
        :title="t('deliveryProgress.blockedTitle')"
        :summary="t('deliveryProgress.blockedDescription')"
        :tone="hasExplicitBlocked ? 'danger' : 'warning'"
      >
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
      </EvidencePanel>
    </section>

    <EvidencePanel
      eyebrow="static evidence"
      :title="t('deliveryProgress.dependencyTitle')"
      :summary="t('deliveryProgress.dependencyDescription')"
    >
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
    </EvidencePanel>

    <section class="delivery-grid">
      <EvidencePanel
        :eyebrow="t('deliveryProgress.completedTitle')"
        :title="t('deliveryProgress.completedTitle')"
        :summary="t('deliveryProgress.completedDescription')"
      >
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
      </EvidencePanel>

      <EvidencePanel
        :eyebrow="t('deliveryProgress.validationTitle')"
        :title="t('deliveryProgress.validationTitle')"
        :summary="t('deliveryProgress.validationDescription')"
      >
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
      </EvidencePanel>
    </section>
  </section>
</template>

<style scoped>
.delivery-progress-page {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-6);
}

.delivery-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--sqlforge-space-6);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: var(--sqlforge-space-4);
}

.task-card,
.module-card,
.timeline-card,
.dependency-card,
.empty-card {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-lg);
  background: var(--sqlforge-surface-2);
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
  padding: var(--sqlforge-space-5);
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

.delivery-section {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-5);
}

.delivery-evidence-rows {
  display: grid;
  gap: var(--sqlforge-space-3);
}

.delivery-evidence-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sqlforge-space-4);
  color: var(--sqlforge-text-secondary);
}

.delivery-evidence-row strong {
  color: var(--sqlforge-text-primary);
  font-weight: 500;
}

@media (max-width: 1280px) {
  .summary-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1080px) {
  .delivery-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 720px) {
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
