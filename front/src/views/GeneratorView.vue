<template>
  <div class="page">
    <el-row :gutter="16">
      <el-col :xs="24" :lg="10">
        <el-card class="section-card left-card">
          <template #header>
            <div class="card-header accent-title">描述生成组件</div>
          </template>

          <el-form label-position="top">
            <div class="form-group subtle-card">
              <el-form-item label="组件名称">
                <el-input v-model="form.componentName" maxlength="80" />
              </el-form-item>
            </div>

            <div class="form-group subtle-card">
              <el-form-item label="需求描述">
                <el-input
                  v-model="form.prompt"
                  type="textarea"
                  :rows="8"
                  maxlength="2000"
                  show-word-limit
                  placeholder="例如：一个带搜索框和分页功能的用户列表表格"
                />
              </el-form-item>
            </div>

            <div class="form-group subtle-card">
              <el-form-item label="约束(JSON)">
                <el-input
                  v-model="form.constraints"
                  type="textarea"
                  :rows="4"
                  placeholder='例如：{"theme":"light","language":"zh-CN"}'
                />
              </el-form-item>
            </div>

            <div class="form-group subtle-card">
              <el-form-item label="演示数据">
                <el-switch v-model="form.includeDemoData" />
                <div class="form-hint">开启后会要求模型注入硬编码示例数据，预览效果更直观。</div>
              </el-form-item>
            </div>
          </el-form>

          <div class="action-row">
            <el-button class="btn-main" type="primary" :loading="loading" @click="handleCreateAndStream">
              生成
            </el-button>
            <el-button class="btn-sub" :disabled="!taskId || loading" @click="handleRegenerate">
              重新生成
            </el-button>
            <el-button class="btn-sub" :disabled="!versionId" @click="handleDownload">下载 .vue</el-button>
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

          <div class="stream-box subtle-card">
            <div class="stream-title accent-title">流式输出</div>
            <pre>{{ streamText || "等待生成中..." }}</pre>
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
import { computed, onMounted, reactive, ref, watch } from "vue";
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
import { useGeneratorDraftStore } from "@/stores/generatorDraft";

const workspaceStore = useWorkspaceStore();
const generatorDraftStore = useGeneratorDraftStore();
const route = useRoute();
const router = useRouter();

const form = reactive({
  prompt: "",
  componentName: "UserTable",
  constraints: "{}",
  includeDemoData: true
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
  return `任务 #${taskId.value} - ${currentStatus.value}`;
});

onMounted(async () => {
  await workspaceStore.ensureWorkspace();
  generatorDraftStore.init();
  hydrateFormFromDraft();
  await initFromQuery();
});

watch(
  () => [form.prompt, form.componentName, form.constraints, form.includeDemoData],
  ([prompt, componentName, constraints, includeDemoData]) => {
    generatorDraftStore.saveDraft({ prompt, componentName, constraints, includeDemoData });
  }
);

async function initFromQuery() {
  const queryTaskId = Number(route.query.taskId);
  const queryVersionId = Number(route.query.versionId);

  if (queryTaskId) {
    taskId.value = queryTaskId;
    const task = await getGenerationTask(taskId.value, workspaceStore.workspaceKey);
    if (task.componentName) {
      form.componentName = task.componentName;
    }
    if (task.prompt) {
      form.prompt = task.prompt;
    }
    if (task.constraints) {
      form.constraints = task.constraints;
    }
    form.includeDemoData = task.includeDemoData !== false;
  }

  if (queryVersionId) {
    versionId.value = queryVersionId;
    const version = await getVersionDetail(versionId.value, workspaceStore.workspaceKey);
    generatedCode.value = version.vueCode || "";
    currentStatus.value = "SUCCEEDED";
  } else if (queryTaskId) {
    const task = await getGenerationTask(taskId.value, workspaceStore.workspaceKey);
    if (task.latestVersion) {
      versionId.value = task.latestVersion.id;
      const version = await getVersionDetail(versionId.value, workspaceStore.workspaceKey);
      generatedCode.value = version.vueCode || "";
      currentStatus.value = "SUCCEEDED";
    }
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
      constraints: form.constraints,
      includeDemoData: form.includeDemoData
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
    errorMessage.value = data.message || "生成失败";
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
  ElMessage.success("开始下载");
}

function hydrateFormFromDraft() {
  form.prompt = generatorDraftStore.prompt;
  form.componentName = generatorDraftStore.componentName;
  form.constraints = generatorDraftStore.constraints;
  form.includeDemoData = generatorDraftStore.includeDemoData !== false;
}
</script>

<style scoped>
.card-header {
  font-weight: 700;
}

.left-card {
  min-height: 100%;
}

.form-group {
  margin-bottom: 12px;
  padding: 10px 12px 2px;
}

.form-hint {
  margin-top: 8px;
  font-size: 12px;
  color: var(--ui-text-muted);
}

.action-row {
  display: flex;
  gap: 10px;
  margin-top: 4px;
}

.btn-main {
  min-width: 120px;
}

.btn-sub {
  color: var(--ui-text-normal);
  border-color: var(--ui-border);
}

.status-box {
  margin-top: 12px;
}

.stream-box {
  margin-top: 12px;
  padding: 8px;
}

.stream-title {
  font-size: 13px;
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
  color: var(--ui-text-normal);
}

.preview-top {
  margin-top: 16px;
}

@media (max-width: 991px) {
  .action-row {
    flex-wrap: wrap;
  }
}
</style>
