import { defineStore } from "pinia";

const THEME_KEY = "uigen_theme_mode";

function normalizeTheme(value) {
  return value === "dark" ? "dark" : "light";
}

export const useThemeStore = defineStore("theme", {
  state: () => ({
    mode: "light",
    initialized: false
  }),
  getters: {
    isDark: (state) => state.mode === "dark"
  },
  actions: {
    init() {
      if (this.initialized) {
        return;
      }
      const saved = localStorage.getItem(THEME_KEY);
      this.mode = normalizeTheme(saved);
      this.applyTheme();
      this.initialized = true;
    },
    toggle() {
      this.mode = this.mode === "dark" ? "light" : "dark";
      this.applyTheme();
      localStorage.setItem(THEME_KEY, this.mode);
    },
    applyTheme() {
      document.documentElement.setAttribute("data-theme", this.mode);
    }
  }
});
