<template>
  <div class="page">
    <el-row :gutter="16">
      <el-col :xs="24" :lg="11">
        <el-card>
          <template #header>
            <div class="header-row">
              <span>Tasks</span>
              <el-button size="small" @click="loadTasks">Refresh</el-button>
            </div>
          </template>
          <el-table :data="tasks" border height="520" @row-click="handleSelectTask">
            <el-table-column prop="id" label="Task" width="80" />
            <el-table-column prop="componentName" label="Component" min-width="140" />
            <el-table-column prop="status" label="Status" width="120" />
            <el-table-column prop="latestVersionNo" label="Latest Ver" width="100" />
            <el-table-column label="Actions" width="190">
              <template #default="{ row }">
                <el-button size="small" @click.stop="handleSelectTask(row)">Versions</el-button>
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
      <el-col :xs="24" :lg="13">
        <el-row :gutter="16">
          <el-col :span="24">
            <el-card>
              <template #header>
                <div class="header-row">
                  <span>Versions of Task #{{ selectedTaskId || "-" }}</span>
                </div>
              </template>
              <el-table :data="versions" border height="220" @row-click="handleSelectVersion">
                <el-table-column prop="versionNo" label="Version" width="90" />
                <el-table-column prop="safetyLevel" label="Safety" width="100" />
                <el-table-column prop="compileOk" label="Compile" width="100">
                  <template #default="{ row }">
                    <el-tag :type="row.compileOk ? 'success' : 'danger'" size="small">
                      {{ row.compileOk ? "OK" : "Fail" }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="createdAt" label="Created At" min-width="180" />
                <el-table-column label="Download" width="120">
                  <template #default="{ row }">
                    <el-button size="small" :disabled="!row.id" @click.stop="handleDownload(row.id)">
                      .vue
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>
          <el-col :span="24" class="mt16">
            <CodeEditorPanel v-model="selectedCode" />
          </el-col>
          <el-col :span="24" class="mt16">
            <PreviewSandbox :code="selectedCode" />
          </el-col>
        </el-row>
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
  getVersionDetail,
  listTaskVersions,
  listWorkspaceTasks
} from "@/api/generation";
import { useWorkspaceStore } from "@/stores/workspace";
import CodeEditorPanel from "@/components/CodeEditorPanel.vue";
import PreviewSandbox from "@/components/PreviewSandbox.vue";

const router = useRouter();
const workspaceStore = useWorkspaceStore();

const tasks = ref([]);
const versions = ref([]);
const selectedTaskId = ref(null);
const selectedCode = ref("");
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
  selectedCode.value = "";
  if (versions.value.length > 0) {
    await handleSelectVersion(versions.value[0]);
  }
}

async function handleSelectVersion(row) {
  const detail = await getVersionDetail(row.id, workspaceStore.workspaceKey);
  selectedCode.value = detail.vueCode || "";
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
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}

.mt16 {
  margin-top: 16px;
}
</style>
