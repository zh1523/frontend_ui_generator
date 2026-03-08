<template>
  <div class="panel">
    <div class="panel-header">
      <h3>生成代码</h3>
      <el-tag size="small" type="info">{{ lineCount }} 行</el-tag>
    </div>
    <el-input
      type="textarea"
      :rows="22"
      :model-value="modelValue"
      @update:model-value="$emit('update:modelValue', $event)"
      class="code-input"
      placeholder="生成的 Vue 组件代码将显示在这里..."
    />
  </div>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  modelValue: {
    type: String,
    default: ""
  }
});

defineEmits(["update:modelValue"]);

const lineCount = computed(() => {
  if (!props.modelValue) {
    return 0;
  }
  return props.modelValue.split("\n").length;
});
</script>

<style scoped>
.panel {
  border: 1px solid var(--ui-border-soft);
  border-radius: var(--ui-radius-md);
  padding: 12px;
  background: var(--ui-card);
  box-shadow: var(--ui-shadow-soft);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--ui-border-soft);
}

.panel-header h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: var(--ui-text-strong);
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.panel-header h3::before {
  content: "";
  width: 4px;
  height: 16px;
  border-radius: 999px;
  background: linear-gradient(180deg, var(--ui-primary), var(--ui-accent));
}

:deep(.code-input textarea) {
  font-family: "Consolas", "Courier New", monospace;
  font-size: 13px;
  border-radius: var(--ui-radius-sm);
  background: #f8fbff;
  color: #17324a;
}
</style>
