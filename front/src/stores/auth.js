import { defineStore } from "pinia";
import { login, logout, me, register } from "@/api/auth";

const AUTH_TOKEN_KEY = "uigen_auth_token";
const AUTH_USER_KEY = "uigen_auth_user";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    token: localStorage.getItem(AUTH_TOKEN_KEY) || "",
    user: readUser(),
    ready: false
  }),
  getters: {
    isAuthed: (state) => Boolean(state.token),
    isAdmin: (state) => state.user?.role === "ADMIN"
  },
  actions: {
    init() {
      this.ready = true;
    },
    async register(payload) {
      const result = await register(payload);
      this.applyAuth(result);
      return result;
    },
    async login(payload) {
      const result = await login(payload);
      this.applyAuth(result);
      return result;
    },
    async refreshMe() {
      if (!this.token) {
        this.user = null;
        return null;
      }
      try {
        const profile = await me();
        this.user = profile;
        localStorage.setItem(AUTH_USER_KEY, JSON.stringify(profile));
        return profile;
      } catch (_) {
        await this.logout();
        return null;
      }
    },
    async logout() {
      try {
        if (this.token) {
          await logout();
        }
      } finally {
        this.token = "";
        this.user = null;
        localStorage.removeItem(AUTH_TOKEN_KEY);
        localStorage.removeItem(AUTH_USER_KEY);
        localStorage.removeItem("uigen_project_id");
        localStorage.removeItem("uigen_workspace_key");
      }
    },
    applyAuth(result) {
      this.token = result.token || "";
      this.user = {
        userId: result.userId,
        username: result.username,
        role: result.role
      };
      localStorage.setItem(AUTH_TOKEN_KEY, this.token);
      localStorage.setItem(AUTH_USER_KEY, JSON.stringify(this.user));
    }
  }
});

function readUser() {
  try {
    const raw = localStorage.getItem(AUTH_USER_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch (_) {
    return null;
  }
}
