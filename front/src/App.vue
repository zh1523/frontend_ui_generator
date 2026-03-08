<template>
  <el-container class="app-shell">
    <el-header class="app-header">
      <div class="header-inner">
        <div class="brand">
          <div class="brand-title">UI Component Generator</div>
          <div class="brand-subtitle">Natural language to Vue component workflow</div>
        </div>
        <div class="header-actions">
          <el-menu class="main-menu" mode="horizontal" :ellipsis="false" :default-active="activeMenu" router>
            <el-menu-item index="/">Generate</el-menu-item>
            <el-menu-item index="/history">History</el-menu-item>
          </el-menu>
          <el-button class="theme-btn" @click="themeStore.toggle">
            {{ themeStore.isDark ? "Light" : "Dark" }}
          </el-button>
        </div>
      </div>
    </el-header>
    <el-main class="app-main">
      <div class="main-inner">
        <router-view />
      </div>
    </el-main>
  </el-container>
</template>

<script setup>
import { computed, onMounted } from "vue";
import { useRoute } from "vue-router";
import { useThemeStore } from "@/stores/theme";

const route = useRoute();
const themeStore = useThemeStore();

onMounted(() => {
  themeStore.init();
});

const activeMenu = computed(() => {
  if (route.path.startsWith("/history")) {
    return "/history";
  }
  return "/";
});
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
}

.app-header {
  border-bottom: 1px solid var(--ui-border-soft);
  background: var(--ui-header-bg);
  backdrop-filter: blur(6px);
  height: 72px;
  display: flex;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 1000;
  box-shadow: var(--ui-shadow-header);
}

.header-inner {
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.brand-title {
  font-size: 18px;
  line-height: 1.2;
  font-weight: 700;
  color: var(--ui-text-strong);
}

.brand-subtitle {
  margin-top: 2px;
  font-size: 12px;
  color: var(--ui-text-subtle);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

:deep(.main-menu.el-menu) {
  border-bottom: none;
  background: transparent;
}

:deep(.main-menu .el-menu-item) {
  border-bottom-width: 3px;
}

.theme-btn {
  min-width: 74px;
  border-color: var(--ui-border);
  color: var(--ui-text-normal);
  background: var(--ui-card);
}

.main-inner {
  max-width: 1400px;
  margin: 0 auto;
  padding: 16px 18px 24px;
}

@media (max-width: 768px) {
  .app-header {
    height: auto;
    min-height: 72px;
    padding: 8px 0;
  }

  .header-inner {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
    justify-content: space-between;
  }

  .main-inner {
    padding: 12px;
  }
}
</style>
