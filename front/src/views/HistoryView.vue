<template>
  <div class="page">
    <div class="page-head">
      <h1 class="page-title">历史版本</h1>
      <p class="page-desc">选择任务后可查看版本列表，点击“打开”跳转到生成页面查看代码与预览。</p>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="12">
        <el-card class="section-card content-card">
          <template #header>
            <div class="header-row">
              <span class="accent-title">任务列表</span>
              <el-button size="small" class="refresh-btn" @click="loadTasks">刷新</el-button>
            </div>
          </template>

          <el-table :data="tasks" border height="560" @row-click="handleSelectTask">
            <el-table-column prop="id" label="任务ID" width="90" />
            <el-table-column prop="componentName" label="组件名" min-width="140" />
            <el-table-column prop="status" label="状态" width="120" />
            <el-table-column prop="latestVersionNo" label="最新版本" width="110" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click.stop="goRegenerate(row)">重新生成</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            class="pager"
            layout="prev, pager, next"
            :current-page="page + 1"
            :page-size="size"
            :total="total"
            @current-change="handlePageChange"
          />
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card class="section-card content-card">
          <template #header>
            <div class="header-row">
              <span class="accent-title">任务 #{{ selectedTaskId || "-" }} 的版本</span>
              <el-tag type="info">点击“打开”跳转到生成页</el-tag>
            </div>
          </template>

          <el-empty v-if="!selectedTaskId" description="请先在左侧选择一个任务" />

          <el-table v-else :data="versions" border height="560">
            <el-table-column prop="versionNo" label="版本号" width="90" />
            <el-table-column prop="safetyLevel" label="安全级别" width="110" />
            <el-table-column prop="compileOk" label="编译结果" width="100">
              <template #default="{ row }">
                <el-tag :type="row.compileOk ? 'success' : 'danger'" size="small">
                  {{ row.compileOk ? "通过" : "失败" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" min-width="180" />
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button size="small" type="primary" class="open-btn" @click.stop="handleOpenVersion(row)">
                  打开
                </el-button>
                <el-button size="small" class="download-btn" :disabled="!row.id" @click.stop="handleDownload(row.id)">
                  下载 .vue
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import {
  downloadVersion,
  listTaskVersions,
  listWorkspaceTasks
} from "@/api/generation";
import { useWorkspaceStore } from "@/stores/workspace";

const router = useRouter();
const workspaceStore = useWorkspaceStore();

const tasks = ref([]);
const versions = ref([]);
const selectedTaskId = ref(null);
const page = ref(0);
const size = ref(10);
const total = ref(0);

onMounted(async () => {
  await workspaceStore.ensureWorkspace();
  await loadTasks();
});

async function loadTasks() {
  const data = await listWorkspaceTasks(workspaceStore.workspaceKey, page.value, size.value);
  tasks.value = data.items || [];
  total.value = data.total || 0;
}

async function handleSelectTask(row) {
  selectedTaskId.value = row.id;
  versions.value = await listTaskVersions(row.id, workspaceStore.workspaceKey);
}

function handleOpenVersion(row) {
  router.push({
    path: "/",
    query: {
      taskId: String(row.taskId || selectedTaskId.value || ""),
      versionId: String(row.id)
    }
  });
}

async function handleDownload(versionId) {
  const blob = await downloadVersion(versionId, workspaceStore.workspaceKey);
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = `component_v${versionId}.vue`;
  anchor.click();
  URL.revokeObjectURL(url);
  ElMessage.success("开始下载");
}

function goRegenerate(row) {
  router.push({
    path: "/",
    query: {
      taskId: row.id,
      regenerate: "1"
    }
  });
}

async function handlePageChange(value) {
  page.value = value - 1;
  await loadTasks();
}
</script>

<style scoped>
.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.page-head {
  margin-bottom: 14px;
}

.content-card {
  min-height: 646px;
}

.refresh-btn,
.download-btn {
  border-color: var(--ui-border);
}

.open-btn {
  min-width: 60px;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}

@media (max-width: 991px) {
  .content-card {
    min-height: auto;
  }
}
</style>
