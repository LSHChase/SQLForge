<script setup>
import { useI18n } from 'vue-i18n'

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
  <section class="route-page">
    <div class="route-card">
      <p class="route-eyebrow">{{ t(props.eyebrowKey) }}</p>
      <h1 class="route-title">{{ t(props.titleKey) }}</h1>
      <p class="route-description">{{ t(props.descriptionKey) }}</p>
    </div>
  </section>
</template>
