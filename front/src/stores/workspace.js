import { defineStore } from "pinia";
import { useProjectStore } from "@/stores/project";

export const useWorkspaceStore = defineStore("workspace", {
  state: () => ({
    workspaceKey: localStorage.getItem("uigen_workspace_key") || "",
    projectId: Number(localStorage.getItem("uigen_project_id") || 0) || null,
    ready: false
  }),
  actions: {
    async ensureWorkspace() {
      const projectStore = useProjectStore();
      await projectStore.ensureProject();
      this.workspaceKey = projectStore.workspaceKey;
      this.projectId = projectStore.projectId;
      this.ready = true;
      return this.workspaceKey;
    },
    async refreshProjects() {
      const projectStore = useProjectStore();
      await projectStore.loadProjects();
      this.workspaceKey = projectStore.workspaceKey;
      this.projectId = projectStore.projectId;
    },
    selectProject(projectId) {
      const projectStore = useProjectStore();
      projectStore.selectProject(projectId);
      this.workspaceKey = projectStore.workspaceKey;
      this.projectId = projectStore.projectId;
    }
  }
});
