import { defineStore } from "pinia";
import { createAnonymousWorkspace } from "@/api/generation";

const WORKSPACE_KEY = "uigen_workspace_key";

export const useWorkspaceStore = defineStore("workspace", {
  state: () => ({
    workspaceKey: localStorage.getItem(WORKSPACE_KEY) || "",
    ready: false
  }),
  actions: {
    async ensureWorkspace() {
      if (this.workspaceKey) {
        this.ready = true;
        return this.workspaceKey;
      }
      const response = await createAnonymousWorkspace();
      this.workspaceKey = response.workspaceKey;
      localStorage.setItem(WORKSPACE_KEY, this.workspaceKey);
      this.ready = true;
      return this.workspaceKey;
    }
  }
});
