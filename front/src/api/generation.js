import { apiRequest, streamSse } from "@/api/client";

const PROJECT_ID_KEY = "uigen_project_id";

export function createAnonymousWorkspace() {
  return apiRequest("/workspaces/anonymous", { method: "POST" });
}

export function createGenerationTask(workspaceKey, payload) {
  return apiRequest("/generations", {
    method: "POST",
    workspaceKey,
    body: payload
  });
}

export function streamGenerationTask(taskId, workspaceKey, options) {
  return streamSse(`/generations/${taskId}/stream`, {
    workspaceKey,
    onEvent: options.onEvent,
    signal: options.signal
  });
}

export function getGenerationTask(taskId, workspaceKey) {
  return apiRequest(`/generations/${taskId}`, { workspaceKey });
}

export function regenerateGenerationTask(taskId, workspaceKey) {
  return apiRequest(`/generations/${taskId}/regenerate`, {
    method: "POST",
    workspaceKey
  });
}

export function listWorkspaceTasks(workspaceKey, page = 0, size = 10) {
  const projectId = localStorage.getItem(PROJECT_ID_KEY);
  if (!projectId) {
    throw new Error("No active project");
  }
  return apiRequest(`/projects/${projectId}/tasks?page=${page}&size=${size}`, { workspaceKey });
}

export function listTaskVersions(taskId, workspaceKey) {
  return apiRequest(`/generations/${taskId}/versions`, { workspaceKey });
}

export function getVersionDetail(versionId, workspaceKey) {
  return apiRequest(`/versions/${versionId}`, { workspaceKey });
}

export function downloadVersion(versionId, workspaceKey) {
  return apiRequest(`/versions/${versionId}/download`, {
    workspaceKey,
    responseType: "blob"
  });
}
