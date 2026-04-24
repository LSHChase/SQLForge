/* eslint-disable no-unused-vars */
import './RuntimeGatesView.css'

import { computed } from 'vue'
import { useI18n } from 'vue-i18n'


const __sfc__ = {
  __name: 'RuntimeGatesView',
  setup(__props, { expose: __expose }) {
  __expose();

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

const __returned__ = { locale, isChinese, gateCards, blockerItems, evidenceRows, computed, get useI18n() { return useI18n } }
Object.defineProperty(__returned__, '__isScriptSetup', { enumerable: false, value: true })
return __returned__
}

}

import { createElementVNode as _createElementVNode, toDisplayString as _toDisplayString, renderList as _renderList, Fragment as _Fragment, openBlock as _openBlock, createElementBlock as _createElementBlock } from "vue"

const _hoisted_1 = {
  class: "runtime-page",
  "data-testid": "runtime-gates-page"
}
const _hoisted_2 = { class: "runtime-hero surface-card" }
const _hoisted_3 = { class: "runtime-title" }
const _hoisted_4 = { class: "runtime-summary" }
const _hoisted_5 = { class: "runtime-note" }
const _hoisted_6 = { class: "summary-card-grid" }
const _hoisted_7 = { class: "section-kicker sqlforge-code-label" }
const _hoisted_8 = { class: "section-title" }
const _hoisted_9 = { class: "section-summary" }
const _hoisted_10 = { class: "check-list" }
const _hoisted_11 = { class: "runtime-grid" }
const _hoisted_12 = { class: "surface-card" }
const _hoisted_13 = { class: "section-heading" }
const _hoisted_14 = { class: "section-title" }
const _hoisted_15 = { class: "evidence-grid" }
const _hoisted_16 = { class: "evidence-label" }
const _hoisted_17 = { class: "surface-card" }
const _hoisted_18 = { class: "section-heading" }
const _hoisted_19 = { class: "section-title" }
const _hoisted_20 = { class: "bullet-list" }

function render(_ctx, _cache, $props, $setup, $data, $options) {
  return (_openBlock(), _createElementBlock("section", _hoisted_1, [
    _createElementVNode("div", _hoisted_2, [
      _createElementVNode("div", null, [
        _cache[0] || (_cache[0] = _createElementVNode("p", { class: "runtime-eyebrow sqlforge-code-label" }, "phase-f runtime gates", -1 /* HOISTED */)),
        _createElementVNode("h1", _hoisted_3, _toDisplayString($setup.isChinese ? '运行时门禁与退出阻断基线' : 'Runtime Gates And Exit Blocking Baseline'), 1 /* TEXT */),
        _createElementVNode("p", _hoisted_4, _toDisplayString($setup.isChinese
              ? '这里收口 Entry / Delivery / Compliance 三层门禁，避免 Phase-F 的 build、runtime、恢复和合规证据继续散在脚本与文档里。'
              : 'This page consolidates the Entry, Delivery, and Compliance gates so Phase-F build, runtime, recovery, and compliance evidence no longer drift across scripts and docs.'), 1 /* TEXT */)
      ]),
      _createElementVNode("p", _hoisted_5, _toDisplayString($setup.isChinese
            ? '当前重点不是再加展示页，而是明确哪些脚本已经成为阻断门禁、哪些仍是残余风险。'
            : 'The point is not more display pages, but a clear line between blocking gates and residual risks.'), 1 /* TEXT */)
    ]),
    _createElementVNode("div", _hoisted_6, [
      (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.gateCards, (gate) => {
        return (_openBlock(), _createElementBlock("article", {
          key: gate.key,
          class: "surface-card gate-card"
        }, [
          _createElementVNode("p", _hoisted_7, _toDisplayString(gate.title), 1 /* TEXT */),
          _createElementVNode("h2", _hoisted_8, _toDisplayString(gate.title), 1 /* TEXT */),
          _createElementVNode("p", _hoisted_9, _toDisplayString(gate.summary), 1 /* TEXT */),
          _createElementVNode("div", _hoisted_10, [
            (_openBlock(true), _createElementBlock(_Fragment, null, _renderList(gate.checks, (item) => {
              return (_openBlock(), _createElementBlock("span", {
                key: item,
                class: "check-pill"
              }, _toDisplayString(item), 1 /* TEXT */))
            }), 128 /* KEYED_FRAGMENT */))
          ])
        ]))
      }), 128 /* KEYED_FRAGMENT */))
    ]),
    _createElementVNode("div", _hoisted_11, [
      _createElementVNode("article", _hoisted_12, [
        _createElementVNode("div", _hoisted_13, [
          _createElementVNode("div", null, [
            _cache[1] || (_cache[1] = _createElementVNode("p", { class: "section-kicker sqlforge-code-label" }, "evidence map", -1 /* HOISTED */)),
            _createElementVNode("h2", _hoisted_14, _toDisplayString($setup.isChinese ? '脚本与 workflow 入口' : 'Scripts And Workflow Entry Points'), 1 /* TEXT */)
          ])
        ]),
        _createElementVNode("div", _hoisted_15, [
          (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.evidenceRows, (row) => {
            return (_openBlock(), _createElementBlock("div", {
              key: row.label,
              class: "evidence-item"
            }, [
              _createElementVNode("span", _hoisted_16, _toDisplayString(row.label), 1 /* TEXT */),
              _createElementVNode("strong", null, _toDisplayString(row.value), 1 /* TEXT */)
            ]))
          }), 128 /* KEYED_FRAGMENT */))
        ])
      ]),
      _createElementVNode("article", _hoisted_17, [
        _createElementVNode("div", _hoisted_18, [
          _createElementVNode("div", null, [
            _cache[2] || (_cache[2] = _createElementVNode("p", { class: "section-kicker sqlforge-code-label" }, "residual blockers", -1 /* HOISTED */)),
            _createElementVNode("h2", _hoisted_19, _toDisplayString($setup.isChinese ? '仍需继续推进的阻塞项' : 'Residual Blockers Still Open'), 1 /* TEXT */)
          ])
        ]),
        _createElementVNode("ul", _hoisted_20, [
          (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.blockerItems, (item) => {
            return (_openBlock(), _createElementBlock("li", { key: item }, _toDisplayString(item), 1 /* TEXT */))
          }), 128 /* KEYED_FRAGMENT */))
        ])
      ])
    ])
  ]))
}
__sfc__.__scopeId = 'data-v-2b5864b6'
__sfc__.render = render
__sfc__.__file = "src/views/runtime-gates/RuntimeGatesView.js"

export default __sfc__
