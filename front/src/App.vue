<template>
  <el-container class="app-shell">
    <el-header class="app-header">
      <div class="header-inner">
        <div class="brand">
          <div class="brand-title">
            前端UI生成工具
            <el-tag size="small" type="success" effect="plain">Beta</el-tag>
          </div>
          <div class="brand-subtitle">自然语言描述生成 Vue 组件代码与预览</div>
        </div>

        <div class="header-actions" v-if="authStore.isAuthed">
          <el-menu
            class="main-menu"
            mode="horizontal"
            :ellipsis="false"
            :default-active="activeMenu"
            @select="handleMenuSelect"
          >
            <el-menu-item index="/">生成</el-menu-item>
            <el-menu-item index="/history">历史版本</el-menu-item>
            <el-menu-item v-if="isAdmin" index="/admin/users">用户管理</el-menu-item>
          </el-menu>

          <el-select
            v-model="selectedProjectId"
            size="small"
            class="project-select"
            placeholder="选择项目"
            @change="handleProjectChange"
          >
            <el-option
              v-for="item in projectStore.projects"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>

          <el-button size="small" class="project-btn" @click="createProjectQuick">新建项目</el-button>

          <el-tooltip v-if="projectStore.costUsage" content="今日 token 配额使用" placement="bottom">
            <el-tag type="warning" effect="plain" class="cost-tag">
              {{ projectStore.costUsage.usedTokens }}/{{ projectStore.costUsage.dailyTokenQuota }}
            </el-tag>
          </el-tooltip>

          <el-button class="theme-btn" size="small" @click="themeStore.toggle">
            {{ themeStore.isDark ? "浅色" : "深色" }}
          </el-button>

          <el-dropdown @command="handleUserCommand">
            <span class="user-menu">{{ authStore.user?.username || "用户" }}</span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
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
import { computed, onMounted, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import { useThemeStore } from "@/stores/theme";
import { useAuthStore } from "@/stores/auth";
import { useProjectStore } from "@/stores/project";

const route = useRoute();
const router = useRouter();
const themeStore = useThemeStore();
const authStore = useAuthStore();
const projectStore = useProjectStore();
const selectedProjectId = ref(null);
const isAdmin = computed(() => authStore.user?.role === "ADMIN");

onMounted(async () => {
  themeStore.init();
  authStore.init();
  if (authStore.isAuthed) {
    await authStore.refreshMe();
    await projectStore.ensureProject();
    selectedProjectId.value = projectStore.projectId;
  }
});

watch(
  () => projectStore.projectId,
  (value) => {
    selectedProjectId.value = value;
  }
);

const activeMenu = computed(() => {
  if (route.path.startsWith("/admin")) {
    return "/admin/users";
  }
  if (route.path.startsWith("/history")) {
    return "/history";
  }
  return "/";
});

function handleMenuSelect(index) {
  if (typeof index !== "string") {
    return;
  }
  if (route.path === index) {
    return;
  }
  router.push(index);
}

async function handleProjectChange(projectId) {
  projectStore.selectProject(projectId);
  await projectStore.refreshCostUsage();
  if (route.path !== "/") {
    await router.push("/");
  }
  ElMessage.success("已切换项目");
}

async function createProjectQuick() {
  try {
    const { value } = await ElMessageBox.prompt("输入项目名称", "新建项目", {
      confirmButtonText: "创建",
      cancelButtonText: "取消",
      inputPattern: /\S+/,
      inputErrorMessage: "项目名称不能为空"
    });
    const created = await projectStore.createProject({
      name: value,
      description: ""
    });
    projectStore.selectProject(created.id);
    await projectStore.refreshCostUsage();
    ElMessage.success("项目创建成功");
  } catch (error) {
    if (error === "cancel") {
      return;
    }
    ElMessage.error(error.message || "创建项目失败");
  }
}

async function handleUserCommand(command) {
  if (command !== "logout") {
    return;
  }
  await authStore.logout();
  projectStore.$reset();
  await router.replace("/login");
}
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
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.brand-subtitle {
  margin-top: 2px;
  font-size: 12px;
  color: var(--ui-text-subtle);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.project-select {
  width: 180px;
}

.project-btn {
  border-color: var(--ui-border);
}

.cost-tag {
  cursor: default;
}

.user-menu {
  cursor: pointer;
  color: var(--ui-text-strong);
  font-size: 13px;
}

:deep(.main-menu.el-menu) {
  border-bottom: none;
  background: transparent;
}

:deep(.main-menu .el-menu-item) {
  border-bottom-width: 3px;
}

.theme-btn {
  min-width: 64px;
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
    flex-wrap: wrap;
  }

  .project-select {
    width: 100%;
  }

  .main-inner {
    padding: 12px;
  }
}
</style>
