<template>
  <el-container class="app-shell">
    <el-header class="app-header">
      <div class="brand">UI Component Generator</div>
      <el-menu mode="horizontal" :ellipsis="false" :default-active="activeMenu" router>
        <el-menu-item index="/">Generate</el-menu-item>
        <el-menu-item index="/history">History</el-menu-item>
      </el-menu>
    </el-header>
    <el-main class="app-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";

const route = useRoute();
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
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #ebeef5;
  background: #fff;
}

.brand {
  font-size: 18px;
  font-weight: 600;
}

.app-main {
  background: #f5f7fa;
}
</style>
