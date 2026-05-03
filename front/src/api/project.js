import { apiRequest } from "@/api/client";

export function createProject(payload) {
  return apiRequest("/projects", {
    method: "POST",
    body: payload
  });
}

export function listProjects() {
  return apiRequest("/projects");
}

export function getProject(projectId) {
  return apiRequest(`/projects/${projectId}`);
}

export function getCostUsage() {
  return apiRequest("/cost/usage");
}
