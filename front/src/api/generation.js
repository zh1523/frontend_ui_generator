import { apiRequest, streamSse } from "@/api/client";

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
  return apiRequest(`/workspaces/me/tasks?page=${page}&size=${size}`, { workspaceKey });
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
