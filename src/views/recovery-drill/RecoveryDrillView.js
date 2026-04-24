/* eslint-disable no-unused-vars */
import './RecoveryDrillView.css'

import { computed } from 'vue'
import { useI18n } from 'vue-i18n'


const __sfc__ = {
  __name: 'RecoveryDrillView',
  setup(__props, { expose: __expose }) {
  __expose();

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

const __returned__ = { locale, isChinese, inventoryItems, objectiveRows, checklistItems, computed, get useI18n() { return useI18n } }
Object.defineProperty(__returned__, '__isScriptSetup', { enumerable: false, value: true })
return __returned__
}

}

import { createElementVNode as _createElementVNode, toDisplayString as _toDisplayString, renderList as _renderList, Fragment as _Fragment, openBlock as _openBlock, createElementBlock as _createElementBlock } from "vue"

const _hoisted_1 = {
  class: "runtime-page",
  "data-testid": "recovery-drill-page"
}
const _hoisted_2 = { class: "runtime-hero surface-card" }
const _hoisted_3 = { class: "runtime-title" }
const _hoisted_4 = { class: "runtime-summary" }
const _hoisted_5 = { class: "runtime-note" }
const _hoisted_6 = { class: "summary-card-grid" }
const _hoisted_7 = { class: "section-kicker sqlforge-code-label" }
const _hoisted_8 = { class: "section-title" }
const _hoisted_9 = { class: "section-summary" }
const _hoisted_10 = { class: "runtime-grid" }
const _hoisted_11 = { class: "surface-card" }
const _hoisted_12 = { class: "section-heading" }
const _hoisted_13 = { class: "section-title" }
const _hoisted_14 = { class: "table-wrap" }
const _hoisted_15 = { class: "objective-table" }
const _hoisted_16 = { class: "surface-card" }
const _hoisted_17 = { class: "section-heading" }
const _hoisted_18 = { class: "section-title" }
const _hoisted_19 = { class: "bullet-list" }

function render(_ctx, _cache, $props, $setup, $data, $options) {
  return (_openBlock(), _createElementBlock("section", _hoisted_1, [
    _createElementVNode("div", _hoisted_2, [
      _createElementVNode("div", null, [
        _cache[0] || (_cache[0] = _createElementVNode("p", { class: "runtime-eyebrow sqlforge-code-label" }, "recovery drill baseline", -1 /* HOISTED */)),
        _createElementVNode("h1", _hoisted_3, _toDisplayString($setup.isChinese ? '备份恢复与恢复后验收基线' : 'Backup Recovery And Post-Restore Acceptance Baseline'), 1 /* TEXT */),
        _createElementVNode("p", _hoisted_4, _toDisplayString($setup.isChinese
              ? '这一页把 F-TASK-008/009 形成的备份对象、恢复目标、责任边界和恢复后检查统一收在治理运维路径里。'
              : 'This page gathers the backup inventory, recovery objectives, ownership boundaries, and post-restore checks established by F-TASK-008/009.'), 1 /* TEXT */)
      ]),
      _createElementVNode("p", _hoisted_5, _toDisplayString($setup.isChinese
            ? '恢复完成的定义不是库导回来了，而是健康探针、审计补偿、队列 backlog、导出/脱敏和敏感泄漏检查都通过。'
            : 'Restore completion means more than database replay: health probes, audit compensation, queue backlog, export/desensitization, and leak checks must all pass.'), 1 /* TEXT */)
    ]),
    _createElementVNode("div", _hoisted_6, [
      (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.inventoryItems, (item) => {
        return (_openBlock(), _createElementBlock("article", {
          key: item.title,
          class: "surface-card inventory-card"
        }, [
          _createElementVNode("p", _hoisted_7, _toDisplayString(item.title), 1 /* TEXT */),
          _createElementVNode("h2", _hoisted_8, _toDisplayString(item.title), 1 /* TEXT */),
          _createElementVNode("p", _hoisted_9, _toDisplayString(item.summary), 1 /* TEXT */)
        ]))
      }), 128 /* KEYED_FRAGMENT */))
    ]),
    _createElementVNode("div", _hoisted_10, [
      _createElementVNode("article", _hoisted_11, [
        _createElementVNode("div", _hoisted_12, [
          _createElementVNode("div", null, [
            _cache[1] || (_cache[1] = _createElementVNode("p", { class: "section-kicker sqlforge-code-label" }, "rpo / rto", -1 /* HOISTED */)),
            _createElementVNode("h2", _hoisted_13, _toDisplayString($setup.isChinese ? '恢复目标与责任人' : 'Recovery Objectives And Owners'), 1 /* TEXT */)
          ])
        ]),
        _createElementVNode("div", _hoisted_14, [
          _createElementVNode("table", _hoisted_15, [
            _createElementVNode("thead", null, [
              _createElementVNode("tr", null, [
                _createElementVNode("th", null, _toDisplayString($setup.isChinese ? '数据域' : 'Domain'), 1 /* TEXT */),
                _cache[2] || (_cache[2] = _createElementVNode("th", null, "RPO", -1 /* HOISTED */)),
                _cache[3] || (_cache[3] = _createElementVNode("th", null, "RTO", -1 /* HOISTED */)),
                _createElementVNode("th", null, _toDisplayString($setup.isChinese ? '责任人' : 'Owner'), 1 /* TEXT */)
              ])
            ]),
            _createElementVNode("tbody", null, [
              (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.objectiveRows, (row) => {
                return (_openBlock(), _createElementBlock("tr", {
                  key: row[0]
                }, [
                  _createElementVNode("td", null, _toDisplayString(row[0]), 1 /* TEXT */),
                  _createElementVNode("td", null, _toDisplayString(row[1]), 1 /* TEXT */),
                  _createElementVNode("td", null, _toDisplayString(row[2]), 1 /* TEXT */),
                  _createElementVNode("td", null, _toDisplayString(row[3]), 1 /* TEXT */)
                ]))
              }), 128 /* KEYED_FRAGMENT */))
            ])
          ])
        ])
      ]),
      _createElementVNode("article", _hoisted_16, [
        _createElementVNode("div", _hoisted_17, [
          _createElementVNode("div", null, [
            _cache[4] || (_cache[4] = _createElementVNode("p", { class: "section-kicker sqlforge-code-label" }, "acceptance checklist", -1 /* HOISTED */)),
            _createElementVNode("h2", _hoisted_18, _toDisplayString($setup.isChinese ? '恢复后必须复验的清单' : 'Mandatory Post-Restore Checklist'), 1 /* TEXT */)
          ])
        ]),
        _createElementVNode("ul", _hoisted_19, [
          (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.checklistItems, (item) => {
            return (_openBlock(), _createElementBlock("li", { key: item }, _toDisplayString(item), 1 /* TEXT */))
          }), 128 /* KEYED_FRAGMENT */))
        ])
      ])
    ])
  ]))
}
__sfc__.__scopeId = 'data-v-be45a667'
__sfc__.render = render
__sfc__.__file = "src/views/recovery-drill/RecoveryDrillView.js"

export default __sfc__
