<template>
  <div class="panel">
    <div class="panel-header">
      <h3>Generated Vue SFC</h3>
      <el-tag size="small" type="info">{{ lineCount }} lines</el-tag>
    </div>
    <el-input
      type="textarea"
      :rows="22"
      :model-value="modelValue"
      @update:model-value="$emit('update:modelValue', $event)"
      class="code-input"
      placeholder="Generated code will appear here..."
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
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 12px;
  background: #fff;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.panel-header h3 {
  margin: 0;
  font-size: 15px;
}

:deep(.code-input textarea) {
  font-family: "Consolas", "Courier New", monospace;
  font-size: 13px;
}
</style>
