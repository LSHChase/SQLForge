/* eslint-disable no-unused-vars */
import { useI18n } from 'vue-i18n'


const __sfc__ = {
  __name: 'RoutePlaceholder',
  props: {
  eyebrowKey: {
    type: String,
    default: 'common.platformTagline'
  },
  titleKey: {
    type: String,
    required: true
  },
  descriptionKey: {
    type: String,
    required: true
  }
},
  setup(__props, { expose: __expose }) {
  __expose();

const props = __props

const { t } = useI18n()

const __returned__ = { props, t, get useI18n() { return useI18n } }
Object.defineProperty(__returned__, '__isScriptSetup', { enumerable: false, value: true })
return __returned__
}

}

import { toDisplayString as _toDisplayString, createElementVNode as _createElementVNode, openBlock as _openBlock, createElementBlock as _createElementBlock } from "vue"

const _hoisted_1 = { class: "route-page" }
const _hoisted_2 = { class: "route-card" }
const _hoisted_3 = { class: "route-eyebrow" }
const _hoisted_4 = { class: "route-title" }
const _hoisted_5 = { class: "route-description" }

function render(_ctx, _cache, $props, $setup, $data, $options) {
  return (_openBlock(), _createElementBlock("section", _hoisted_1, [
    _createElementVNode("div", _hoisted_2, [
      _createElementVNode("p", _hoisted_3, _toDisplayString($setup.t($setup.props.eyebrowKey)), 1 /* TEXT */),
      _createElementVNode("h1", _hoisted_4, _toDisplayString($setup.t($setup.props.titleKey)), 1 /* TEXT */),
      _createElementVNode("p", _hoisted_5, _toDisplayString($setup.t($setup.props.descriptionKey)), 1 /* TEXT */)
    ])
  ]))
}
__sfc__.render = render
__sfc__.__file = "src/views/common/RoutePlaceholder.js"

export default __sfc__
