<script setup>
import { useI18n } from 'vue-i18n'
import PageHero from './PageHero.vue'

const props = defineProps({
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
})

const { t } = useI18n()
</script>

<template>
  <PageHero
    class="route-page"
    :eyebrow="t(props.eyebrowKey)"
    :title="t(props.titleKey)"
    :summary="t(props.descriptionKey)"
  />
</template>
