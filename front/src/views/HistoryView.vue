<template>
  <div class="page">
    <div class="page-head">
      <h1 class="page-title">History & Versions</h1>
      <p class="page-desc">Select a task, browse version records, open a version in generator page, or download source file.</p>
    </div>
    <el-row :gutter="16">
      <el-col :xs="24" :lg="12">
        <el-card class="section-card content-card">
          <template #header>
            <div class="header-row">
              <span class="accent-title">Tasks</span>
              <el-button size="small" class="refresh-btn" @click="loadTasks">Refresh</el-button>
            </div>
          </template>
          <el-table :data="tasks" border height="560" @row-click="handleSelectTask">
            <el-table-column prop="id" label="Task" width="80" />
            <el-table-column prop="componentName" label="Component" min-width="140" />
            <el-table-column prop="status" label="Status" width="120" />
            <el-table-column prop="latestVersionNo" label="Latest Ver" width="100" />
            <el-table-column label="Actions" width="120">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click.stop="goRegenerate(row)">Regenerate</el-button>
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
              <span class="accent-title">Versions of Task #{{ selectedTaskId || "-" }}</span>
              <el-tag type="info">Use Open button to jump to Generator</el-tag>
            </div>
          </template>
          <el-empty v-if="!selectedTaskId" description="Select a task to view versions" />
          <el-table v-else :data="versions" border height="560">
            <el-table-column prop="versionNo" label="Version" width="90" />
            <el-table-column prop="safetyLevel" label="Safety" width="110" />
            <el-table-column prop="compileOk" label="Compile" width="100">
              <template #default="{ row }">
                <el-tag :type="row.compileOk ? 'success' : 'danger'" size="small">
                  {{ row.compileOk ? "OK" : "Fail" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="Created At" min-width="180" />
            <el-table-column label="Actions" width="180">
              <template #default="{ row }">
                <el-button size="small" type="primary" class="open-btn" @click.stop="handleOpenVersion(row)">
                  Open
                </el-button>
                <el-button size="small" class="download-btn" :disabled="!row.id" @click.stop="handleDownload(row.id)">
                  .vue
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
  ElMessage.success("Download started");
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
