<template>
  <div class="panel">
    <div class="panel-header">
      <h3>预览区域</h3>
      <el-tag :type="unsafe ? 'danger' : 'success'" size="small">
        {{ unsafe ? "已拦截" : "安全" }}
      </el-tag>
    </div>
    <div v-if="!code" class="empty">暂无可预览代码</div>
    <div v-else-if="unsafe" class="empty">
      该代码触发前端沙箱策略，已被拦截。
    </div>
    <div v-else class="preview-wrapper">
      <component :is="runtimeComponent" />
    </div>
  </div>
</template>

<script setup>
import { computed, defineComponent, reactive } from "vue";
import { detectUnsafeCode, extractSimpleState, extractStyleBody, extractTemplateBody, parseSfcSections } from "@/utils/sfc";

const props = defineProps({
  code: {
    type: String,
    default: ""
  }
});

const unsafe = computed(() => detectUnsafeCode(props.code));

const runtimeComponent = computed(() => {
  const sections = parseSfcSections(props.code);
  const template = extractTemplateBody(sections.template) || "<div>预览不可用</div>";
  const style = extractStyleBody(sections.style);
  const parsedState = extractSimpleState(sections.script);
  const { fallbackState, fallbackMethods } = buildTemplateFallbacks(template, parsedState);
  return defineComponent({
    name: "RuntimePreview",
    template: `<div class="preview-host">${template}</div>`,
    data() {
      return reactive({
        ...parsedState,
        ...fallbackState
      });
    },
    methods: {
      ...fallbackMethods
    },
    mounted() {
      applyStyle(style);
    },
    unmounted() {
      removeStyle();
    }
  });
});

function applyStyle(cssText) {
  removeStyle();
  const styleTag = document.createElement("style");
  styleTag.id = "preview-runtime-style";
  styleTag.textContent = cssText;
  document.head.appendChild(styleTag);
}

function removeStyle() {
  const styleTag = document.getElementById("preview-runtime-style");
  if (styleTag) {
    styleTag.remove();
  }
}

function buildTemplateFallbacks(template, currentState) {
  const fallbackState = {};
  const fallbackMethods = {};
  const handlerKeys = findHandlerKeys(template);
  const modelKeys = findModelKeys(template);
  const listKeys = findVForSourceKeys(template);
  const allKeys = new Set([...modelKeys, ...listKeys, ...findInterpolationKeys(template)]);

  for (const key of handlerKeys) {
    if (typeof currentState[key] !== "function") {
      fallbackMethods[key] = () => {};
    }
  }

  for (const key of allKeys) {
    if (Object.hasOwn(currentState, key)) {
      continue;
    }
    fallbackState[key] = guessDefaultValue(key, listKeys.has(key));
  }

  return { fallbackState, fallbackMethods };
}

function findHandlerKeys(template) {
  const keys = new Set();
  const regex = /(?:@[\w.-]+|v-on:[\w.-]+)\s*=\s*"([^"]+)"/g;
  for (const match of template.matchAll(regex)) {
    const expr = match[1].trim();
    const handler = expr.match(/^([A-Za-z_]\w*)\s*(?:\(|$)/);
    if (handler) {
      keys.add(handler[1]);
    }
  }
  return keys;
}

function findModelKeys(template) {
  const keys = new Set();
  const regex = /v-model(?:\.[\w-]+)*\s*=\s*"([^"]+)"/g;
  for (const match of template.matchAll(regex)) {
    const key = toTopLevelKey(match[1]);
    if (key) {
      keys.add(key);
    }
  }
  return keys;
}

function findVForSourceKeys(template) {
  const keys = new Set();
  const regex = /v-for\s*=\s*"([^"]+)"/g;
  for (const match of template.matchAll(regex)) {
    const expr = match[1];
    const inMatch = expr.match(/\bin\b\s*([A-Za-z_]\w*)/);
    if (inMatch) {
      keys.add(inMatch[1]);
    }
  }
  return keys;
}

function findInterpolationKeys(template) {
  const keys = new Set();
  const regex = /{{\s*([^}]+)\s*}}/g;
  for (const match of template.matchAll(regex)) {
    const expr = match[1];
    for (const token of expr.match(/[A-Za-z_]\w*/g) || []) {
      if (!RESERVED_IDENTIFIERS.has(token)) {
        keys.add(token);
      }
    }
  }
  return keys;
}

function toTopLevelKey(expr) {
  const clean = (expr || "").trim();
  const found = clean.match(/^([A-Za-z_]\w*)/);
  return found ? found[1] : "";
}

function guessDefaultValue(key, forceArray) {
  if (forceArray || /(list|items|data|rows)$/i.test(key)) {
    return [];
  }
  if (/^(page|current|total|count|size|index|length)/i.test(key) || /(page|total|count|size)$/i.test(key)) {
    return 0;
  }
  if (/^(is|has|can|show|visible|enabled)/i.test(key)) {
    return false;
  }
  return "";
}

const RESERVED_IDENTIFIERS = new Set([
  "true",
  "false",
  "null",
  "undefined",
  "Math",
  "Date",
  "Number",
  "String",
  "Boolean",
  "Object",
  "Array"
]);
</script>

<style scoped>
.panel {
  border: 1px solid var(--ui-border-soft);
  border-radius: var(--ui-radius-md);
  padding: 12px;
  background: var(--ui-card);
  min-height: 480px;
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

.empty {
  color: var(--ui-text-subtle);
  padding: 24px 0;
}

.preview-wrapper {
  border: 1px dashed #bfd8ec;
  border-radius: var(--ui-radius-sm);
  min-height: 400px;
  padding: 12px;
  background: linear-gradient(180deg, #f8fbff, #f4fbff);
  overflow: auto;
}
</style>
