/* eslint-disable no-unused-vars */
import './DeliveryProgressView.css'

import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { createDeliveryProgressSnapshot } from './progressSnapshot'
import { deliveryProgressAvailability } from '../../config/runtimeFlags'


const __sfc__ = {
  __name: 'DeliveryProgressView',
  setup(__props, { expose: __expose }) {
  __expose();

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

const __returned__ = { t, locale, snapshot, summaryCards, sourceFiles, activeTasks, completedTasks, moduleProgress, recentChanges, blockerItems, dependencyChains, validationEntries, hasExplicitBlocked, runtimeAvailability, runtimePills, statusTypeMap, changeKindTypeMap, formatProgressLog, computed, get useI18n() { return useI18n }, get createDeliveryProgressSnapshot() { return createDeliveryProgressSnapshot }, get deliveryProgressAvailability() { return deliveryProgressAvailability } }
Object.defineProperty(__returned__, '__isScriptSetup', { enumerable: false, value: true })
return __returned__
}

}

import { toDisplayString as _toDisplayString, createElementVNode as _createElementVNode, renderList as _renderList, Fragment as _Fragment, openBlock as _openBlock, createElementBlock as _createElementBlock, normalizeClass as _normalizeClass, createTextVNode as _createTextVNode, resolveComponent as _resolveComponent, withCtx as _withCtx, createVNode as _createVNode, normalizeStyle as _normalizeStyle, createCommentVNode as _createCommentVNode } from "vue"

const _hoisted_1 = { class: "delivery-progress-page" }
const _hoisted_2 = { class: "delivery-hero" }
const _hoisted_3 = { class: "delivery-hero-copy" }
const _hoisted_4 = { class: "hero-eyebrow sqlforge-code-label" }
const _hoisted_5 = { class: "hero-title" }
const _hoisted_6 = { class: "hero-summary" }
const _hoisted_7 = { class: "hero-pills" }
const _hoisted_8 = { class: "delivery-hero-card" }
const _hoisted_9 = { class: "delivery-hero-label sqlforge-code-label" }
const _hoisted_10 = { class: "hero-card-list" }
const _hoisted_11 = { class: "hero-card-row" }
const _hoisted_12 = { class: "hero-card-row" }
const _hoisted_13 = { class: "hero-card-row" }
const _hoisted_14 = { class: "hero-card-footnote" }
const _hoisted_15 = { class: "delivery-section" }
const _hoisted_16 = { class: "section-heading" }
const _hoisted_17 = { class: "section-kicker sqlforge-code-label" }
const _hoisted_18 = { class: "sqlforge-section-title" }
const _hoisted_19 = { class: "section-summary" }
const _hoisted_20 = { class: "summary-grid" }
const _hoisted_21 = { class: "summary-card-label" }
const _hoisted_22 = { class: "summary-card-value" }
const _hoisted_23 = { class: "delivery-grid" }
const _hoisted_24 = { class: "delivery-section" }
const _hoisted_25 = { class: "section-heading" }
const _hoisted_26 = { class: "section-kicker sqlforge-code-label" }
const _hoisted_27 = { class: "sqlforge-section-title" }
const _hoisted_28 = { class: "section-summary" }
const _hoisted_29 = { class: "task-list" }
const _hoisted_30 = { class: "task-card-header" }
const _hoisted_31 = { class: "task-card-id sqlforge-code-label" }
const _hoisted_32 = { class: "task-card-title" }
const _hoisted_33 = { class: "task-card-scope" }
const _hoisted_34 = { class: "task-card-meta" }
const _hoisted_35 = { class: "task-card-log" }
const _hoisted_36 = { class: "delivery-section" }
const _hoisted_37 = { class: "section-heading" }
const _hoisted_38 = { class: "section-kicker sqlforge-code-label" }
const _hoisted_39 = { class: "sqlforge-section-title" }
const _hoisted_40 = { class: "section-summary" }
const _hoisted_41 = { class: "module-list" }
const _hoisted_42 = { class: "module-card-header" }
const _hoisted_43 = { class: "module-card-label sqlforge-code-label" }
const _hoisted_44 = { class: "module-card-total" }
const _hoisted_45 = { class: "module-card-bar" }
const _hoisted_46 = { class: "module-card-stats" }
const _hoisted_47 = { class: "delivery-grid" }
const _hoisted_48 = { class: "delivery-section" }
const _hoisted_49 = { class: "section-heading" }
const _hoisted_50 = { class: "section-kicker sqlforge-code-label" }
const _hoisted_51 = { class: "sqlforge-section-title" }
const _hoisted_52 = { class: "section-summary" }
const _hoisted_53 = { class: "timeline-list" }
const _hoisted_54 = { class: "timeline-card-header" }
const _hoisted_55 = { class: "timeline-card-id sqlforge-code-label" }
const _hoisted_56 = { class: "timeline-card-line" }
const _hoisted_57 = { class: "timeline-card-meta" }
const _hoisted_58 = {
  key: 0,
  class: "timeline-card-line timeline-card-line-muted"
}
const _hoisted_59 = { class: "delivery-section" }
const _hoisted_60 = { class: "section-heading" }
const _hoisted_61 = { class: "section-kicker sqlforge-code-label" }
const _hoisted_62 = { class: "sqlforge-section-title" }
const _hoisted_63 = { class: "section-summary" }
const _hoisted_64 = {
  key: 0,
  class: "task-list"
}
const _hoisted_65 = { class: "task-card-header" }
const _hoisted_66 = { class: "task-card-id sqlforge-code-label" }
const _hoisted_67 = { class: "task-card-title" }
const _hoisted_68 = { class: "task-card-log" }
const _hoisted_69 = { class: "task-card-meta" }
const _hoisted_70 = {
  key: 1,
  class: "empty-card"
}
const _hoisted_71 = { class: "section-footnote" }
const _hoisted_72 = { class: "delivery-section" }
const _hoisted_73 = { class: "section-heading" }
const _hoisted_74 = { class: "section-kicker sqlforge-code-label" }
const _hoisted_75 = { class: "sqlforge-section-title" }
const _hoisted_76 = { class: "section-summary" }
const _hoisted_77 = {
  key: 0,
  class: "dependency-list"
}
const _hoisted_78 = { class: "dependency-card-header" }
const _hoisted_79 = { class: "timeline-card-id sqlforge-code-label" }
const _hoisted_80 = { class: "dependency-card-tags" }
const _hoisted_81 = { class: "dependency-count" }
const _hoisted_82 = { class: "dependency-chip-list" }
const _hoisted_83 = { class: "dependency-chip-header" }
const _hoisted_84 = {
  key: 1,
  class: "empty-card"
}
const _hoisted_85 = { class: "delivery-grid" }
const _hoisted_86 = { class: "delivery-section" }
const _hoisted_87 = { class: "section-heading" }
const _hoisted_88 = { class: "section-kicker sqlforge-code-label" }
const _hoisted_89 = { class: "sqlforge-section-title" }
const _hoisted_90 = { class: "section-summary" }
const _hoisted_91 = { class: "timeline-list" }
const _hoisted_92 = { class: "timeline-card-header" }
const _hoisted_93 = { class: "timeline-card-id sqlforge-code-label" }
const _hoisted_94 = { class: "timeline-card-time" }
const _hoisted_95 = { class: "timeline-card-line" }
const _hoisted_96 = { class: "delivery-section" }
const _hoisted_97 = { class: "section-heading" }
const _hoisted_98 = { class: "section-kicker sqlforge-code-label" }
const _hoisted_99 = { class: "sqlforge-section-title" }
const _hoisted_100 = { class: "section-summary" }
const _hoisted_101 = { class: "timeline-list" }
const _hoisted_102 = { class: "timeline-card-header" }
const _hoisted_103 = { class: "timeline-card-id sqlforge-code-label" }
const _hoisted_104 = { class: "timeline-card-time" }
const _hoisted_105 = { class: "timeline-card-line" }
const _hoisted_106 = { class: "timeline-card-line timeline-card-line-muted" }

function render(_ctx, _cache, $props, $setup, $data, $options) {
  const _component_el_tag = _resolveComponent("el-tag")

  return (_openBlock(), _createElementBlock("section", _hoisted_1, [
    _createElementVNode("section", _hoisted_2, [
      _createElementVNode("div", _hoisted_3, [
        _createElementVNode("p", _hoisted_4, _toDisplayString($setup.t('deliveryProgress.eyebrow')), 1 /* TEXT */),
        _createElementVNode("h1", _hoisted_5, _toDisplayString($setup.t('deliveryProgress.heroTitle')), 1 /* TEXT */),
        _createElementVNode("p", _hoisted_6, _toDisplayString($setup.t('deliveryProgress.heroSummary')), 1 /* TEXT */),
        _createElementVNode("div", _hoisted_7, [
          (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.runtimePills, (pill) => {
            return (_openBlock(), _createElementBlock("span", {
              key: pill.key,
              class: "hero-pill hero-pill-strong"
            }, _toDisplayString(pill.label), 1 /* TEXT */))
          }), 128 /* KEYED_FRAGMENT */)),
          (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.sourceFiles, (sourceFile) => {
            return (_openBlock(), _createElementBlock("span", {
              key: sourceFile,
              class: "hero-pill"
            }, _toDisplayString(sourceFile), 1 /* TEXT */))
          }), 128 /* KEYED_FRAGMENT */))
        ])
      ]),
      _createElementVNode("div", _hoisted_8, [
        _createElementVNode("p", _hoisted_9, _toDisplayString($setup.t('deliveryProgress.visibilityLabel')), 1 /* TEXT */),
        _createElementVNode("h2", null, _toDisplayString($setup.t('deliveryProgress.visibilityTitle')), 1 /* TEXT */),
        _createElementVNode("p", null, _toDisplayString($setup.t($setup.runtimeAvailability.visibilityReasonKey)), 1 /* TEXT */),
        _createElementVNode("div", _hoisted_10, [
          _createElementVNode("div", _hoisted_11, [
            _createElementVNode("span", null, _toDisplayString($setup.t('deliveryProgress.runtime.modeLabel')), 1 /* TEXT */),
            _createElementVNode("strong", null, _toDisplayString($setup.runtimeAvailability.mode), 1 /* TEXT */)
          ]),
          _createElementVNode("div", _hoisted_12, [
            _createElementVNode("span", null, _toDisplayString($setup.t('deliveryProgress.runtime.flagLabel')), 1 /* TEXT */),
            _createElementVNode("strong", null, _toDisplayString($setup.t(`deliveryProgress.runtime.flag.${$setup.runtimeAvailability.flagState}`)), 1 /* TEXT */)
          ]),
          _createElementVNode("div", _hoisted_13, [
            _createElementVNode("span", null, _toDisplayString($setup.t('deliveryProgress.runtime.scopeLabel')), 1 /* TEXT */),
            _createElementVNode("strong", null, _toDisplayString($setup.t('deliveryProgress.runtime.scope')), 1 /* TEXT */)
          ])
        ]),
        _createElementVNode("p", _hoisted_14, _toDisplayString($setup.t('deliveryProgress.visibilitySummary')), 1 /* TEXT */)
      ])
    ]),
    _createElementVNode("section", _hoisted_15, [
      _createElementVNode("div", _hoisted_16, [
        _createElementVNode("div", null, [
          _createElementVNode("p", _hoisted_17, _toDisplayString($setup.t('deliveryProgress.summaryTitle')), 1 /* TEXT */),
          _createElementVNode("h2", _hoisted_18, _toDisplayString($setup.t('deliveryProgress.summaryTitle')), 1 /* TEXT */)
        ]),
        _createElementVNode("p", _hoisted_19, _toDisplayString($setup.t('deliveryProgress.summaryDescription')), 1 /* TEXT */)
      ]),
      _createElementVNode("div", _hoisted_20, [
        (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.summaryCards, (card) => {
          return (_openBlock(), _createElementBlock("article", {
            key: card.key,
            class: _normalizeClass(["summary-card", `summary-card-${card.tone}`])
          }, [
            _createElementVNode("p", _hoisted_21, _toDisplayString($setup.t(`deliveryProgress.cards.${card.key}`)), 1 /* TEXT */),
            _createElementVNode("p", _hoisted_22, _toDisplayString(card.value), 1 /* TEXT */)
          ], 2 /* CLASS */))
        }), 128 /* KEYED_FRAGMENT */))
      ])
    ]),
    _createElementVNode("section", _hoisted_23, [
      _createElementVNode("section", _hoisted_24, [
        _createElementVNode("div", _hoisted_25, [
          _createElementVNode("div", null, [
            _createElementVNode("p", _hoisted_26, _toDisplayString($setup.t('deliveryProgress.activeTasksTitle')), 1 /* TEXT */),
            _createElementVNode("h2", _hoisted_27, _toDisplayString($setup.t('deliveryProgress.activeTasksTitle')), 1 /* TEXT */)
          ]),
          _createElementVNode("p", _hoisted_28, _toDisplayString($setup.t('deliveryProgress.activeTasksDescription')), 1 /* TEXT */)
        ]),
        _createElementVNode("div", _hoisted_29, [
          (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.activeTasks, (task) => {
            return (_openBlock(), _createElementBlock("article", {
              key: task.taskId,
              class: "task-card"
            }, [
              _createElementVNode("div", _hoisted_30, [
                _createElementVNode("div", null, [
                  _createElementVNode("p", _hoisted_31, _toDisplayString(task.taskId), 1 /* TEXT */),
                  _createElementVNode("h3", _hoisted_32, _toDisplayString(task.name), 1 /* TEXT */)
                ]),
                _createVNode(_component_el_tag, {
                  type: $setup.statusTypeMap[task.status]
                }, {
                  default: _withCtx(() => [
                    _createTextVNode(_toDisplayString($setup.t(`deliveryProgress.status.${task.statusKey}`)), 1 /* TEXT */)
                  ]),
                  _: 2 /* DYNAMIC */
                }, 1032 /* PROPS, DYNAMIC_SLOTS */, ["type"])
              ]),
              _createElementVNode("p", _hoisted_33, _toDisplayString(task.scope), 1 /* TEXT */),
              _createElementVNode("div", _hoisted_34, [
                _createElementVNode("span", null, _toDisplayString($setup.t('deliveryProgress.meta.priority')) + ": " + _toDisplayString(task.priority), 1 /* TEXT */),
                _createElementVNode("span", null, _toDisplayString($setup.t('deliveryProgress.meta.dependsOn')) + ": " + _toDisplayString(task.dependsOn), 1 /* TEXT */)
              ]),
              _createElementVNode("p", _hoisted_35, _toDisplayString($setup.formatProgressLog(task.progressLog)), 1 /* TEXT */)
            ]))
          }), 128 /* KEYED_FRAGMENT */))
        ])
      ]),
      _createElementVNode("section", _hoisted_36, [
        _createElementVNode("div", _hoisted_37, [
          _createElementVNode("div", null, [
            _createElementVNode("p", _hoisted_38, _toDisplayString($setup.t('deliveryProgress.moduleTitle')), 1 /* TEXT */),
            _createElementVNode("h2", _hoisted_39, _toDisplayString($setup.t('deliveryProgress.moduleTitle')), 1 /* TEXT */)
          ]),
          _createElementVNode("p", _hoisted_40, _toDisplayString($setup.t('deliveryProgress.moduleDescription')), 1 /* TEXT */)
        ]),
        _createElementVNode("div", _hoisted_41, [
          (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.moduleProgress, (module) => {
            return (_openBlock(), _createElementBlock("article", {
              key: module.moduleLabel,
              class: "module-card"
            }, [
              _createElementVNode("div", _hoisted_42, [
                _createElementVNode("div", null, [
                  _createElementVNode("p", _hoisted_43, _toDisplayString(module.moduleLabel), 1 /* TEXT */),
                  _createElementVNode("h3", null, _toDisplayString(module.completionRate) + "%", 1 /* TEXT */)
                ]),
                _createElementVNode("p", _hoisted_44, _toDisplayString(module.total) + " " + _toDisplayString($setup.t('deliveryProgress.meta.totalTasks')), 1 /* TEXT */)
              ]),
              _createElementVNode("div", _hoisted_45, [
                _createElementVNode("div", {
                  class: "module-card-bar-fill",
                  style: _normalizeStyle({ width: `${module.completionRate}%` })
                }, null, 4 /* STYLE */)
              ]),
              _createElementVNode("div", _hoisted_46, [
                _createElementVNode("span", null, _toDisplayString($setup.t('deliveryProgress.status.done')) + " " + _toDisplayString(module.done), 1 /* TEXT */),
                _createElementVNode("span", null, _toDisplayString($setup.t('deliveryProgress.status.in_progress')) + " " + _toDisplayString(module.in_progress), 1 /* TEXT */),
                _createElementVNode("span", null, _toDisplayString($setup.t('deliveryProgress.status.in_review')) + " " + _toDisplayString(module.in_review), 1 /* TEXT */),
                _createElementVNode("span", null, _toDisplayString($setup.t('deliveryProgress.status.todo')) + " " + _toDisplayString(module.todo), 1 /* TEXT */),
                _createElementVNode("span", null, _toDisplayString($setup.t('deliveryProgress.status.blocked')) + " " + _toDisplayString(module.blocked), 1 /* TEXT */)
              ])
            ]))
          }), 128 /* KEYED_FRAGMENT */))
        ])
      ])
    ]),
    _createElementVNode("section", _hoisted_47, [
      _createElementVNode("section", _hoisted_48, [
        _createElementVNode("div", _hoisted_49, [
          _createElementVNode("div", null, [
            _createElementVNode("p", _hoisted_50, _toDisplayString($setup.t('deliveryProgress.recentChangesTitle')), 1 /* TEXT */),
            _createElementVNode("h2", _hoisted_51, _toDisplayString($setup.t('deliveryProgress.recentChangesTitle')), 1 /* TEXT */)
          ]),
          _createElementVNode("p", _hoisted_52, _toDisplayString($setup.t('deliveryProgress.recentChangesDescription')), 1 /* TEXT */)
        ]),
        _createElementVNode("div", _hoisted_53, [
          (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.recentChanges, (item) => {
            return (_openBlock(), _createElementBlock("article", {
              key: item.itemKey,
              class: "timeline-card"
            }, [
              _createElementVNode("div", _hoisted_54, [
                _createElementVNode("div", null, [
                  _createElementVNode("p", _hoisted_55, _toDisplayString(item.taskId), 1 /* TEXT */),
                  _createElementVNode("h3", null, _toDisplayString(item.title), 1 /* TEXT */)
                ]),
                _createVNode(_component_el_tag, {
                  type: $setup.changeKindTypeMap[item.kind]
                }, {
                  default: _withCtx(() => [
                    _createTextVNode(_toDisplayString($setup.t(`deliveryProgress.changeKind.${item.kind}`)), 1 /* TEXT */)
                  ]),
                  _: 2 /* DYNAMIC */
                }, 1032 /* PROPS, DYNAMIC_SLOTS */, ["type"])
              ]),
              _createElementVNode("p", _hoisted_56, _toDisplayString(item.detail), 1 /* TEXT */),
              _createElementVNode("div", _hoisted_57, [
                _createElementVNode("span", null, _toDisplayString(item.timestampLabel), 1 /* TEXT */),
                _createElementVNode("span", null, _toDisplayString(item.sourceFile), 1 /* TEXT */)
              ]),
              (item.auxiliary)
                ? (_openBlock(), _createElementBlock("p", _hoisted_58, _toDisplayString(item.auxiliary), 1 /* TEXT */))
                : _createCommentVNode("v-if", true)
            ]))
          }), 128 /* KEYED_FRAGMENT */))
        ])
      ]),
      _createElementVNode("section", _hoisted_59, [
        _createElementVNode("div", _hoisted_60, [
          _createElementVNode("div", null, [
            _createElementVNode("p", _hoisted_61, _toDisplayString($setup.t('deliveryProgress.blockedTitle')), 1 /* TEXT */),
            _createElementVNode("h2", _hoisted_62, _toDisplayString($setup.t('deliveryProgress.blockedTitle')), 1 /* TEXT */)
          ]),
          _createElementVNode("p", _hoisted_63, _toDisplayString($setup.t('deliveryProgress.blockedDescription')), 1 /* TEXT */)
        ]),
        ($setup.blockerItems.length)
          ? (_openBlock(), _createElementBlock("div", _hoisted_64, [
              (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.blockerItems, (item) => {
                return (_openBlock(), _createElementBlock("article", {
                  key: item.taskId,
                  class: "task-card task-card-danger"
                }, [
                  _createElementVNode("div", _hoisted_65, [
                    _createElementVNode("div", null, [
                      _createElementVNode("p", _hoisted_66, _toDisplayString(item.taskId), 1 /* TEXT */),
                      _createElementVNode("h3", _hoisted_67, _toDisplayString(item.name), 1 /* TEXT */)
                    ]),
                    _createVNode(_component_el_tag, {
                      type: $setup.statusTypeMap[item.status]
                    }, {
                      default: _withCtx(() => [
                        _createTextVNode(_toDisplayString($setup.t(`deliveryProgress.status.${item.status}`)), 1 /* TEXT */)
                      ]),
                      _: 2 /* DYNAMIC */
                    }, 1032 /* PROPS, DYNAMIC_SLOTS */, ["type"])
                  ]),
                  _createElementVNode("p", _hoisted_68, _toDisplayString(item.reason), 1 /* TEXT */),
                  _createElementVNode("div", _hoisted_69, [
                    _createElementVNode("span", null, _toDisplayString($setup.t('deliveryProgress.meta.dependsOn')) + ": " + _toDisplayString(item.dependsOn), 1 /* TEXT */),
                    _createElementVNode("span", null, _toDisplayString($setup.t('deliveryProgress.meta.blockedSource')), 1 /* TEXT */)
                  ])
                ]))
              }), 128 /* KEYED_FRAGMENT */))
            ]))
          : (_openBlock(), _createElementBlock("article", _hoisted_70, [
              _createElementVNode("h3", null, _toDisplayString($setup.t('deliveryProgress.empty.blockedTitle')), 1 /* TEXT */),
              _createElementVNode("p", null, _toDisplayString($setup.t('deliveryProgress.empty.blockedDescription')), 1 /* TEXT */)
            ])),
        _createElementVNode("p", _hoisted_71, _toDisplayString($setup.hasExplicitBlocked
              ? $setup.t('deliveryProgress.blockedFootnote')
              : $setup.t('deliveryProgress.pendingFootnote')), 1 /* TEXT */)
      ])
    ]),
    _createElementVNode("section", _hoisted_72, [
      _createElementVNode("div", _hoisted_73, [
        _createElementVNode("div", null, [
          _createElementVNode("p", _hoisted_74, _toDisplayString($setup.t('deliveryProgress.dependencyTitle')), 1 /* TEXT */),
          _createElementVNode("h2", _hoisted_75, _toDisplayString($setup.t('deliveryProgress.dependencyTitle')), 1 /* TEXT */)
        ]),
        _createElementVNode("p", _hoisted_76, _toDisplayString($setup.t('deliveryProgress.dependencyDescription')), 1 /* TEXT */)
      ]),
      ($setup.dependencyChains.length)
        ? (_openBlock(), _createElementBlock("div", _hoisted_77, [
            (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.dependencyChains, (chain) => {
              return (_openBlock(), _createElementBlock("article", {
                key: chain.taskId,
                class: "dependency-card"
              }, [
                _createElementVNode("div", _hoisted_78, [
                  _createElementVNode("div", null, [
                    _createElementVNode("p", _hoisted_79, _toDisplayString(chain.taskId), 1 /* TEXT */),
                    _createElementVNode("h3", null, _toDisplayString(chain.name), 1 /* TEXT */)
                  ]),
                  _createElementVNode("div", _hoisted_80, [
                    _createVNode(_component_el_tag, {
                      type: $setup.statusTypeMap[chain.status]
                    }, {
                      default: _withCtx(() => [
                        _createTextVNode(_toDisplayString($setup.t(`deliveryProgress.status.${chain.statusKey}`)), 1 /* TEXT */)
                      ]),
                      _: 2 /* DYNAMIC */
                    }, 1032 /* PROPS, DYNAMIC_SLOTS */, ["type"]),
                    _createElementVNode("span", _hoisted_81, _toDisplayString($setup.t('deliveryProgress.meta.unresolvedCount', { count: chain.unresolvedCount })), 1 /* TEXT */)
                  ])
                ]),
                _createElementVNode("div", _hoisted_82, [
                  (_openBlock(true), _createElementBlock(_Fragment, null, _renderList(chain.dependencies, (dependency) => {
                    return (_openBlock(), _createElementBlock("article", {
                      key: `${chain.taskId}-${dependency.dependencyId}`,
                      class: "dependency-chip"
                    }, [
                      _createElementVNode("div", _hoisted_83, [
                        _createElementVNode("strong", null, _toDisplayString(dependency.dependencyId), 1 /* TEXT */),
                        _createVNode(_component_el_tag, {
                          size: "small",
                          type: $setup.statusTypeMap[dependency.status]
                        }, {
                          default: _withCtx(() => [
                            _createTextVNode(_toDisplayString($setup.t(`deliveryProgress.status.${dependency.statusKey}`)), 1 /* TEXT */)
                          ]),
                          _: 2 /* DYNAMIC */
                        }, 1032 /* PROPS, DYNAMIC_SLOTS */, ["type"])
                      ]),
                      _createElementVNode("p", null, _toDisplayString(dependency.label), 1 /* TEXT */),
                      _createElementVNode("span", null, _toDisplayString($setup.t(`deliveryProgress.dependencySource.${dependency.source}`)), 1 /* TEXT */)
                    ]))
                  }), 128 /* KEYED_FRAGMENT */))
                ])
              ]))
            }), 128 /* KEYED_FRAGMENT */))
          ]))
        : (_openBlock(), _createElementBlock("article", _hoisted_84, [
            _createElementVNode("h3", null, _toDisplayString($setup.t('deliveryProgress.empty.dependencyTitle')), 1 /* TEXT */),
            _createElementVNode("p", null, _toDisplayString($setup.t('deliveryProgress.empty.dependencyDescription')), 1 /* TEXT */)
          ]))
    ]),
    _createElementVNode("section", _hoisted_85, [
      _createElementVNode("section", _hoisted_86, [
        _createElementVNode("div", _hoisted_87, [
          _createElementVNode("div", null, [
            _createElementVNode("p", _hoisted_88, _toDisplayString($setup.t('deliveryProgress.completedTitle')), 1 /* TEXT */),
            _createElementVNode("h2", _hoisted_89, _toDisplayString($setup.t('deliveryProgress.completedTitle')), 1 /* TEXT */)
          ]),
          _createElementVNode("p", _hoisted_90, _toDisplayString($setup.t('deliveryProgress.completedDescription')), 1 /* TEXT */)
        ]),
        _createElementVNode("div", _hoisted_91, [
          (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.completedTasks, (task) => {
            return (_openBlock(), _createElementBlock("article", {
              key: task.taskId,
              class: "timeline-card"
            }, [
              _createElementVNode("div", _hoisted_92, [
                _createElementVNode("div", null, [
                  _createElementVNode("p", _hoisted_93, _toDisplayString(task.taskId), 1 /* TEXT */),
                  _createElementVNode("h3", null, _toDisplayString(task.name), 1 /* TEXT */)
                ]),
                _createElementVNode("span", _hoisted_94, _toDisplayString(task.completedAt), 1 /* TEXT */)
              ]),
              _createElementVNode("p", _hoisted_95, _toDisplayString(task.commitSubject), 1 /* TEXT */)
            ]))
          }), 128 /* KEYED_FRAGMENT */))
        ])
      ]),
      _createElementVNode("section", _hoisted_96, [
        _createElementVNode("div", _hoisted_97, [
          _createElementVNode("div", null, [
            _createElementVNode("p", _hoisted_98, _toDisplayString($setup.t('deliveryProgress.validationTitle')), 1 /* TEXT */),
            _createElementVNode("h2", _hoisted_99, _toDisplayString($setup.t('deliveryProgress.validationTitle')), 1 /* TEXT */)
          ]),
          _createElementVNode("p", _hoisted_100, _toDisplayString($setup.t('deliveryProgress.validationDescription')), 1 /* TEXT */)
        ]),
        _createElementVNode("div", _hoisted_101, [
          (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.validationEntries, (entry) => {
            return (_openBlock(), _createElementBlock("article", {
              key: `${entry.timestamp}-${entry.trigger}`,
              class: "timeline-card"
            }, [
              _createElementVNode("div", _hoisted_102, [
                _createElementVNode("div", null, [
                  _createElementVNode("p", _hoisted_103, _toDisplayString(entry.trigger), 1 /* TEXT */),
                  _createElementVNode("h3", null, _toDisplayString(entry.result), 1 /* TEXT */)
                ]),
                _createElementVNode("span", _hoisted_104, _toDisplayString(entry.timestamp), 1 /* TEXT */)
              ]),
              _createElementVNode("p", _hoisted_105, _toDisplayString(entry.rules), 1 /* TEXT */),
              _createElementVNode("p", _hoisted_106, _toDisplayString(entry.evidence), 1 /* TEXT */)
            ]))
          }), 128 /* KEYED_FRAGMENT */))
        ])
      ])
    ])
  ]))
}
__sfc__.__scopeId = 'data-v-1e055eb1'
__sfc__.render = render
__sfc__.__file = "src/views/delivery/DeliveryProgressView.js"

export default __sfc__
