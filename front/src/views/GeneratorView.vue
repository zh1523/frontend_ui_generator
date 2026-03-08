<template>
  <div class="page">
    <el-row :gutter="16">
      <el-col :xs="24" :lg="10">
        <el-card>
          <template #header>
            <div class="card-header">Prompt to Component</div>
          </template>
          <el-form label-position="top">
            <el-form-item label="Component Name">
              <el-input v-model="form.componentName" maxlength="80" />
            </el-form-item>
            <el-form-item label="Prompt">
              <el-input
                v-model="form.prompt"
                type="textarea"
                :rows="8"
                maxlength="2000"
                show-word-limit
                placeholder="e.g. A user table with search box and pagination"
              />
            </el-form-item>
            <el-form-item label="Constraints(JSON)">
              <el-input
                v-model="form.constraints"
                type="textarea"
                :rows="4"
                placeholder='e.g. {"theme":"light","language":"zh-CN"}'
              />
            </el-form-item>
          </el-form>
          <div class="action-row">
            <el-button type="primary" :loading="loading" @click="handleCreateAndStream">
              Generate
            </el-button>
            <el-button :disabled="!taskId || loading" @click="handleRegenerate">
              Regenerate
            </el-button>
            <el-button :disabled="!versionId" @click="handleDownload">Download .vue</el-button>
          </div>
          <el-alert
            v-if="errorMessage"
            type="error"
            :closable="false"
            :title="errorMessage"
            show-icon
            class="status-box"
          />
          <el-alert
            v-else-if="statusLabel"
            type="info"
            :closable="false"
            :title="statusLabel"
            show-icon
            class="status-box"
          />
          <div class="stream-box">
            <div class="stream-title">SSE Stream</div>
            <pre>{{ streamText || "Waiting for generation..." }}</pre>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="14">
        <el-row :gutter="16">
          <el-col :span="24">
            <CodeEditorPanel v-model="generatedCode" />
          </el-col>
          <el-col :span="24" class="preview-top">
            <PreviewSandbox :code="generatedCode" />
          </el-col>
        </el-row>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import CodeEditorPanel from "@/components/CodeEditorPanel.vue";
import PreviewSandbox from "@/components/PreviewSandbox.vue";
import {
  createGenerationTask,
  downloadVersion,
  getGenerationTask,
  getVersionDetail,
  regenerateGenerationTask,
  streamGenerationTask
} from "@/api/generation";
import { useWorkspaceStore } from "@/stores/workspace";

const workspaceStore = useWorkspaceStore();
const route = useRoute();
const router = useRouter();

const form = reactive({
  prompt: "一个带搜索框和分页功能的用户列表表格",
  componentName: "UserTable",
  constraints: "{}"
});

const taskId = ref(null);
const versionId = ref(null);
const loading = ref(false);
const errorMessage = ref("");
const streamText = ref("");
const generatedCode = ref("");
const currentStatus = ref("IDLE");
let abortController = null;

const statusLabel = computed(() => {
  if (!taskId.value) {
    return "";
  }
  return `Task #${taskId.value} - ${currentStatus.value}`;
});

onMounted(async () => {
  await workspaceStore.ensureWorkspace();
  await initFromQuery();
});

async function initFromQuery() {
  const queryTaskId = Number(route.query.taskId);
  if (!queryTaskId) {
    return;
  }
  taskId.value = queryTaskId;
  const task = await getGenerationTask(taskId.value, workspaceStore.workspaceKey);
  if (task.latestVersion) {
    versionId.value = task.latestVersion.id;
    const version = await getVersionDetail(versionId.value, workspaceStore.workspaceKey);
    generatedCode.value = version.vueCode;
  }
  if (route.query.regenerate === "1") {
    await handleRegenerate();
    await router.replace({ path: "/" });
  }
}

async function handleCreateAndStream() {
  errorMessage.value = "";
  streamText.value = "";
  generatedCode.value = "";
  versionId.value = null;
  try {
    const created = await createGenerationTask(workspaceStore.workspaceKey, {
      prompt: form.prompt,
      componentName: form.componentName,
      constraints: form.constraints
    });
    taskId.value = created.taskId;
    await startStream(created.taskId);
  } catch (error) {
    errorMessage.value = error.message;
  }
}

async function handleRegenerate() {
  if (!taskId.value) {
    return;
  }
  errorMessage.value = "";
  streamText.value = "";
  generatedCode.value = "";
  versionId.value = null;
  try {
    await regenerateGenerationTask(taskId.value, workspaceStore.workspaceKey);
    await startStream(taskId.value);
  } catch (error) {
    errorMessage.value = error.message;
  }
}

async function startStream(targetTaskId) {
  loading.value = true;
  currentStatus.value = "GENERATING";
  if (abortController) {
    abortController.abort();
  }
  abortController = new AbortController();
  try {
    await streamGenerationTask(targetTaskId, workspaceStore.workspaceKey, {
      signal: abortController.signal,
      onEvent: onSseEvent
    });
  } catch (error) {
    errorMessage.value = error.message;
    currentStatus.value = "FAILED";
  } finally {
    loading.value = false;
  }
}

function onSseEvent({ event, data }) {
  if (event === "started") {
    currentStatus.value = "GENERATING";
    return;
  }
  if (event === "token") {
    streamText.value += data.token || "";
    return;
  }
  if (event === "partial_code") {
    if (data.text) {
      streamText.value = data.text;
    }
    return;
  }
  if (event === "final_code") {
    generatedCode.value = data.code || "";
    versionId.value = data.versionId || null;
    currentStatus.value = "SUCCEEDED";
    return;
  }
  if (event === "error") {
    errorMessage.value = data.message || "Generation failed";
    currentStatus.value = "FAILED";
    return;
  }
  if (event === "done") {
    currentStatus.value = data.status || "DONE";
  }
}

async function handleDownload() {
  if (!versionId.value) {
    return;
  }
  const blob = await downloadVersion(versionId.value, workspaceStore.workspaceKey);
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = `${form.componentName || "GeneratedComponent"}.vue`;
  anchor.click();
  URL.revokeObjectURL(url);
  ElMessage.success("Download started");
}
</script>

<style scoped>
.card-header {
  font-weight: 600;
}

.action-row {
  display: flex;
  gap: 10px;
  margin-top: 6px;
}

.status-box {
  margin-top: 12px;
}

.stream-box {
  margin-top: 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 8px;
  background: #fafafa;
}

.stream-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 6px;
}

.stream-box pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 280px;
  overflow: auto;
  font-family: "Consolas", "Courier New", monospace;
  font-size: 12px;
}

.preview-top {
  margin-top: 16px;
}
</style>
