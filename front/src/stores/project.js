import { defineStore } from "pinia";
import { createProject, getCostUsage, listProjects } from "@/api/project";
import { useAuthStore } from "@/stores/auth";

const PROJECT_ID_KEY = "uigen_project_id";
const WORKSPACE_KEY = "uigen_workspace_key";

export const useProjectStore = defineStore("project", {
  state: () => ({
    projects: [],
    projectId: Number(localStorage.getItem(PROJECT_ID_KEY) || 0) || null,
    workspaceKey: localStorage.getItem(WORKSPACE_KEY) || "",
    costUsage: null,
    ready: false
  }),
  getters: {
    currentProject(state) {
      if (!state.projectId) {
        return null;
      }
      return state.projects.find((item) => item.id === state.projectId) || null;
    }
  },
  actions: {
    async ensureProject() {
      const authStore = useAuthStore();
      if (!authStore.isAuthed) {
        this.projects = [];
        this.projectId = null;
        this.workspaceKey = "";
        this.costUsage = null;
        this.ready = true;
        return "";
      }

      await this.loadProjects();
      if (!this.projectId && this.projects.length > 0) {
        this.selectProject(this.projects[0].id);
      }
      if (!this.projectId) {
        const created = await this.createProject({
          name: "Default Project",
          description: "Auto created project"
        });
        this.selectProject(created.id);
      }
      await this.refreshCostUsage();
      this.ready = true;
      return this.workspaceKey;
    },

    async loadProjects() {
      this.projects = await listProjects();
      if (this.projectId) {
        const exists = this.projects.some((item) => item.id === this.projectId);
        if (!exists) {
          this.projectId = null;
          this.workspaceKey = "";
          localStorage.removeItem(PROJECT_ID_KEY);
          localStorage.removeItem(WORKSPACE_KEY);
        }
      }
      if (!this.projectId && this.projects.length > 0) {
        this.selectProject(this.projects[0].id);
      }
    },

    async createProject(payload) {
      const created = await createProject(payload);
      this.projects.unshift(created);
      return created;
    },

    selectProject(projectId) {
      const project = this.projects.find((item) => item.id === Number(projectId));
      if (!project) {
        return;
      }
      this.projectId = project.id;
      this.workspaceKey = project.workspaceKey;
      localStorage.setItem(PROJECT_ID_KEY, String(project.id));
      localStorage.setItem(WORKSPACE_KEY, project.workspaceKey);
    },

    async refreshCostUsage() {
      try {
        this.costUsage = await getCostUsage();
      } catch (_) {
        this.costUsage = null;
      }
    }
  }
});
