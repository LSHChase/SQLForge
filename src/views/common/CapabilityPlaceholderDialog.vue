<script setup>
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: ''
  },
  capability: {
    type: String,
    default: ''
  },
  reason: {
    type: String,
    default: ''
  },
  nextStep: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue'])

const handleClose = () => {
  emit('update:modelValue', false)
}
</script>

<template>
  <el-dialog
    :model-value="props.modelValue"
    :title="props.title"
    width="620px"
    @close="handleClose"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="placeholder-stack">
      <div class="placeholder-card">
        <span class="placeholder-label">Capability</span>
        <strong>{{ props.capability || '-' }}</strong>
      </div>
      <div class="placeholder-card">
        <span class="placeholder-label">Current status</span>
        <strong>{{ props.reason || 'The current repository does not expose a writable API for this action.' }}</strong>
      </div>
      <div v-if="props.nextStep" class="placeholder-card">
        <span class="placeholder-label">Next step</span>
        <strong>{{ props.nextStep }}</strong>
      </div>
    </div>
    <template #footer>
      <el-button @click="handleClose">Close</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.placeholder-stack {
  display: grid;
  gap: 12px;
}

.placeholder-card {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid var(--sqlforge-border-default);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.03), transparent 34%),
    var(--sqlforge-surface-2);
}

.placeholder-label {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--sqlforge-text-muted);
}
</style>
