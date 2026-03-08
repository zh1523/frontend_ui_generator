import { defineStore } from "pinia";

const DRAFT_KEY = "uigen_generator_draft_v1";

function loadDraft() {
  try {
    const raw = localStorage.getItem(DRAFT_KEY);
    if (!raw) {
      return null;
    }
    return JSON.parse(raw);
  } catch (_) {
    return null;
  }
}

export const useGeneratorDraftStore = defineStore("generatorDraft", {
  state: () => ({
    prompt: "",
    componentName: "UserTable",
    constraints: "{}",
    initialized: false
  }),
  actions: {
    init() {
      if (this.initialized) {
        return;
      }
      const draft = loadDraft();
      if (draft) {
        this.prompt = draft.prompt || "";
        this.componentName = draft.componentName || "UserTable";
        this.constraints = draft.constraints || "{}";
      } else {
        this.prompt = "A user table with search box and pagination";
        this.componentName = "UserTable";
        this.constraints = "{}";
      }
      this.initialized = true;
    },
    saveDraft(payload) {
      this.prompt = payload.prompt ?? this.prompt;
      this.componentName = payload.componentName ?? this.componentName;
      this.constraints = payload.constraints ?? this.constraints;
      localStorage.setItem(
        DRAFT_KEY,
        JSON.stringify({
          prompt: this.prompt,
          componentName: this.componentName,
          constraints: this.constraints
        })
      );
    }
  }
});
