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
      该代码触发前端沙箱规则，已被拦截。
    </div>
    <div v-else-if="previewError" class="empty">
      预览运行失败：{{ previewError }}
    </div>
    <div v-else class="preview-wrapper">
      <component :is="runtimeComponent" v-if="runtimeComponent" />
    </div>
  </div>
</template>

<script setup>
import {
  computed,
  defineComponent,
  nextTick,
  onBeforeUnmount,
  onErrorCaptured,
  onMounted,
  onUnmounted,
  reactive,
  readonly,
  ref,
  shallowRef,
  toRef,
  toRefs,
  watch,
  watchEffect
} from "vue";
import {
  detectUnsafeCode,
  extractSimpleState,
  extractStyleBody,
  extractTemplateBody,
  parseSfcSections
} from "@/utils/sfc";

const props = defineProps({
  code: {
    type: String,
    default: ""
  }
});

const previewError = ref("");
const styleTagId = `preview-runtime-style-${Math.random().toString(36).slice(2)}`;

const unsafe = computed(() => detectUnsafeCode(props.code));

const runtimeComponent = computed(() => {
  if (!props.code || unsafe.value) {
    return null;
  }

  try {
    const sections = parseSfcSections(props.code);
    const template = extractTemplateBody(sections.template) || "<div>预览不可用</div>";
    const style = extractStyleBody(sections.style);
    const runtimeState = evaluateScriptSetup(sections.script) || extractSimpleState(sections.script);
    const { fallbackState, fallbackMethods } = buildTemplateFallbacks(template, runtimeState);
    const numericCallKeys = findToFixedTargets(template);

    return defineComponent({
      name: "RuntimePreview",
      template: `<div class=\"preview-host\">${template}</div>`,
      setup() {
        const merged = {
          ...runtimeState,
          ...fallbackState,
          ...fallbackMethods
        };
        const enriched = enrichPreviewState(merged);
        const normalized = normalizeStateTypes(enriched);
        forceNumericKeys(normalized, numericCallKeys);
        return normalized;
      },
      mounted() {
        applyStyle(style);
      },
      unmounted() {
        removeStyle();
      }
    });
  } catch (error) {
    previewError.value = normalizeError(error);
    removeStyle();
    return null;
  }
});

watch(
  () => props.code,
  () => {
    previewError.value = "";
    removeStyle();
  }
);

onErrorCaptured((error) => {
  previewError.value = normalizeError(error);
  return false;
});

onBeforeUnmount(() => {
  removeStyle();
});

function applyStyle(cssText) {
  removeStyle();
  const styleTag = document.createElement("style");
  styleTag.id = styleTagId;
  styleTag.textContent = scopeCssToHost(cssText, ".preview-host");
  document.head.appendChild(styleTag);
}

function removeStyle() {
  const styleTag = document.getElementById(styleTagId);
  if (styleTag) {
    styleTag.remove();
  }
}

function evaluateScriptSetup(scriptTag) {
  const body = (scriptTag || "")
    .replace(/<script[^>]*>/i, "")
    .replace(/<\/script>/i, "")
    .trim();
  if (!body) {
    return {};
  }
  if (/export\s+default/.test(body)) {
    return null;
  }

  const executable = body
    .replace(/^\s*import\s+[^;]+;?\s*$/gm, "")
    .replace(/\bdefineProps\s*<[^>]*>\s*\(/g, "defineProps(")
    .replace(/\bdefineEmits\s*<[^>]*>\s*\(/g, "defineEmits(")
    .trim();
  if (!executable) {
    return {};
  }

  const exposeNames = extractTopLevelNames(executable);
  const fn = new Function(
    "ref",
    "computed",
    "reactive",
    "readonly",
    "shallowRef",
    "watch",
    "watchEffect",
    "onMounted",
    "onUnmounted",
    "nextTick",
    "toRef",
    "toRefs",
    "defineProps",
    "defineEmits",
    `"use strict";
${executable}
return { ${exposeNames.join(",")} };`
  );

  try {
    return fn(
      ref,
      computed,
      reactive,
      readonly,
      shallowRef,
      watch,
      watchEffect,
      onMounted,
      onUnmounted,
      nextTick,
      toRef,
      toRefs,
      () => ({}),
      () => () => {}
    );
  } catch (_) {
    return null;
  }
}

function extractTopLevelNames(script) {
  const names = new Set();
  const declRegex = /\b(?:const|let|var)\s+([A-Za-z_]\w*)/g;
  for (const match of script.matchAll(declRegex)) {
    names.add(match[1]);
  }
  const funcRegex = /\bfunction\s+([A-Za-z_]\w*)\s*\(/g;
  for (const match of script.matchAll(funcRegex)) {
    names.add(match[1]);
  }
  for (const forbidden of RESERVED_IDENTIFIERS) {
    names.delete(forbidden);
  }
  return Array.from(names);
}

function buildTemplateFallbacks(template, currentState) {
  const fallbackState = {};
  const fallbackMethods = {};
  const handlerKeys = findHandlerKeys(template);
  const callKeys = findCalledFunctionKeys(template);
  const modelKeys = findModelKeys(template);
  const listKeys = findVForSourceKeys(template);
  const expressionKeys = findTemplateExpressionKeys(template);
  const allKeys = new Set([...modelKeys, ...listKeys, ...expressionKeys, ...findInterpolationKeys(template)]);

  for (const key of new Set([...handlerKeys, ...callKeys])) {
    if (typeof currentState[key] !== "function") {
      fallbackMethods[key] = createFallbackMethod(key);
    }
  }

  for (const key of allKeys) {
    if (Object.hasOwn(currentState, key)) {
      continue;
    }
    fallbackState[key] = guessDefaultValue(key, listKeys.has(key));
  }

  applyDerivedListFallbacks(currentState, fallbackState, allKeys);
  return { fallbackState, fallbackMethods };
}

function findCalledFunctionKeys(template) {
  const keys = new Set();
  for (const expr of collectTemplateExpressions(template)) {
    for (const match of expr.matchAll(/\b([A-Za-z_]\w*)\s*\(/g)) {
      const fnName = match[1];
      if (RESERVED_IDENTIFIERS.has(fnName)) {
        continue;
      }
      const index = match.index ?? 0;
      const prevChar = index > 0 ? expr[index - 1] : "";
      if (prevChar === ".") {
        continue;
      }
      keys.add(fnName);
    }
  }
  return keys;
}

function collectTemplateExpressions(template) {
  const expressions = [];

  const interpolationRegex = /{{\s*([^}]+)\s*}}/g;
  for (const match of template.matchAll(interpolationRegex)) {
    expressions.push(match[1]);
  }

  const directiveRegex = /(?:v-if|v-else-if|v-show|v-bind:[\w.-]+|:[\w.-]+)\s*=\s*"([^"]+)"/g;
  for (const match of template.matchAll(directiveRegex)) {
    expressions.push(match[1]);
  }

  return expressions;
}

function createFallbackMethod(name) {
  if (/(list|items|data|rows|records|options|products|users|orders|cart)/i.test(name)) {
    return () => [];
  }
  if (/(price|amount|cost|fee|tax|subtotal|total|count|qty|quantity|num|rate|score|index|size|length)$/i.test(name)) {
    return () => 0;
  }
  if (/^(is|has|can|should|allow|enabled|visible)/i.test(name)) {
    return () => false;
  }
  if (/style/i.test(name)) {
    return () => ({});
  }
  return () => "";
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

function findTemplateExpressionKeys(template) {
  const keys = new Set();
  const regex = /(?:v-if|v-else-if|v-show|v-bind:[\w.-]+|:[\w.-]+)\s*=\s*"([^"]+)"/g;
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
  if (forceArray || /(list|items|data|rows|records|options|products|users|orders|cart)/i.test(key)) {
    return [];
  }
  if (/(price|amount|cost|fee|tax|subtotal|total|count|qty|quantity|num|rate|score|index|size|length)$/i.test(key)) {
    return 0;
  }
  if (/^(page|current|total|count|size|index|length)/i.test(key)) {
    return 0;
  }
  if (/^(is|has|can|show|visible|enabled)/i.test(key)) {
    return false;
  }
  return "";
}

function applyDerivedListFallbacks(currentState, fallbackState, keys) {
  const arrayEntries = Object.entries(currentState).filter(([, value]) => Array.isArray(value));
  if (arrayEntries.length === 0) {
    return;
  }
  for (const key of keys) {
    if (Object.hasOwn(currentState, key)) {
      continue;
    }
    if (!Array.isArray(fallbackState[key])) {
      continue;
    }
    const sourceArray = resolveSourceArray(key, currentState, arrayEntries);
    if (sourceArray) {
      fallbackState[key] = sourceArray;
    }
  }
}

function resolveSourceArray(key, currentState, arrayEntries) {
  const directCandidates = [];
  directCandidates.push(key.replace(/^(filtered|paginated|sorted)/i, ""));
  directCandidates.push(key.replace(/^(visible|displayed)/i, ""));
  directCandidates.push(key.replace(/Items$/i, ""));

  for (const candidate of directCandidates) {
    const normalized = normalizeVarName(candidate);
    if (!normalized) {
      continue;
    }
    if (Array.isArray(currentState[normalized])) {
      return currentState[normalized];
    }
    for (const [name, value] of arrayEntries) {
      if (name.toLowerCase().includes(normalized.toLowerCase())) {
        return value;
      }
    }
  }

  // Prefer menu-like arrays before generic first array.
  const menuEntry = arrayEntries.find(([name]) => /menu|product|goods/i.test(name));
  if (menuEntry) {
    return menuEntry[1];
  }
  return arrayEntries[0][1];
}

function normalizeVarName(raw) {
  const input = (raw || "").trim();
  if (!input) {
    return "";
  }
  return input.charAt(0).toLowerCase() + input.slice(1);
}

function normalizeStateTypes(state) {
  const normalized = {};
  for (const [key, value] of Object.entries(state || {})) {
    normalized[key] = normalizeValueByKey(key, value);
  }
  return normalized;
}

function normalizeValueByKey(key, value) {
  if (isVueRef(value)) {
    return value;
  }
  if (isNumericLikeKey(key)) {
    if (typeof value === "number" && Number.isFinite(value)) {
      return value;
    }
    if (typeof value === "string") {
      const parsed = Number(value);
      if (Number.isFinite(parsed)) {
        return parsed;
      }
      return 0;
    }
    if (value === null || value === undefined || typeof value === "boolean") {
      return 0;
    }
    if (Array.isArray(value) || typeof value === "object") {
      return 0;
    }
  }
  return value;
}

function isNumericLikeKey(key) {
  if (!key) {
    return false;
  }
  return /(price|amount|cost|fee|tax|subtotal|total|count|qty|quantity|num|rate|score|index|size|length)$/i.test(key)
      || /^(page|current|total|count|size|index|length|cartTotal|grandTotal)/i.test(key);
}

function findToFixedTargets(template) {
  const keys = new Set();
  const regex = /\b([A-Za-z_]\w*)\s*\.\s*toFixed\s*\(/g;
  for (const match of template.matchAll(regex)) {
    keys.add(match[1]);
  }
  return keys;
}

function forceNumericKeys(state, keys) {
  for (const key of keys) {
    if (isVueRef(state[key])) {
      continue;
    }
    state[key] = toFiniteNumber(state[key], 0);
  }
}

function enrichPreviewState(state) {
  const next = { ...state };
  const menuItems = Array.isArray(next.menuItems) ? next.menuItems : [];
  const cart = Array.isArray(next.cart) ? next.cart : [];

  if (Array.isArray(next.filteredItems) && next.filteredItems.length === 0 && menuItems.length > 0) {
    next.filteredItems = deriveFilteredItems(next, menuItems);
  }

  if (Array.isArray(next.cartItems)) {
    next.cartItems = deriveCartItems(cart, menuItems, next.cartItems);
  } else if (cart.length > 0 || menuItems.length > 0) {
    next.cartItems = deriveCartItems(cart, menuItems, []);
  }

  if (next.cartCount === undefined || next.cartCount === null || Number.isNaN(Number(next.cartCount))) {
    next.cartCount = deriveCartCount(cart, next.cartItems);
  }

  if (next.cartTotal === undefined || next.cartTotal === null || Number.isNaN(Number(next.cartTotal))) {
    next.cartTotal = deriveCartTotal(next.cartItems);
  }

  return next;
}

function deriveFilteredItems(state, menuItems) {
  if (state.activeCategory && state.activeCategory !== "all") {
    return menuItems.filter((item) => item && item.categoryId === state.activeCategory);
  }
  return menuItems;
}

function deriveCartItems(cart, menuItems, existingCartItems) {
  const productMap = new Map(menuItems.map((item) => [item?.id, item]));
  const base = Array.isArray(existingCartItems) && existingCartItems.length > 0 ? existingCartItems : cart;
  return base.map((entry) => {
    const product = productMap.get(entry?.id) || {};
    const qty = toFiniteNumber(entry?.qty, 1);
    const price = toFiniteNumber(entry?.price ?? product.price, 0);
    return {
      ...product,
      ...entry,
      id: entry?.id ?? product.id,
      qty,
      price
    };
  });
}

function deriveCartCount(cart, cartItems) {
  if (Array.isArray(cart) && cart.length > 0) {
    return cart.reduce((sum, item) => sum + toFiniteNumber(item?.qty, 0), 0);
  }
  if (Array.isArray(cartItems) && cartItems.length > 0) {
    return cartItems.reduce((sum, item) => sum + toFiniteNumber(item?.qty, 0), 0);
  }
  return 0;
}

function deriveCartTotal(cartItems) {
  if (!Array.isArray(cartItems)) {
    return 0;
  }
  return cartItems.reduce(
    (sum, item) => sum + toFiniteNumber(item?.price, 0) * toFiniteNumber(item?.qty, 0),
    0
  );
}

function toFiniteNumber(value, fallback = 0) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

function isVueRef(value) {
  return value && typeof value === "object" && Object.prototype.hasOwnProperty.call(value, "value");
}

function normalizeError(error) {
  if (!error) {
    return "未知错误";
  }
  if (typeof error === "string") {
    return error;
  }
  return error.message || "运行时错误";
}

function scopeCssToHost(cssText, hostSelector) {
  if (!cssText || !cssText.trim()) {
    return "";
  }
  return cssText.replace(/(^|})\s*([^@}{][^{}]*)\{/g, (full, boundary, selectorPart) => {
    const scopedSelectors = selectorPart
      .split(",")
      .map((raw) => raw.trim())
      .filter(Boolean)
      .map((selector) => {
        if (selector.startsWith(hostSelector)) {
          return selector;
        }
        if (selector === "body" || selector === "html" || selector === ":root") {
          return hostSelector;
        }
        return `${hostSelector} ${selector}`;
      })
      .join(", ");
    return `${boundary} ${scopedSelectors} {`;
  });
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
