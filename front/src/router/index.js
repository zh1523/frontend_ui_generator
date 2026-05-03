import { createRouter, createWebHistory } from "vue-router";
import GeneratorView from "@/views/GeneratorView.vue";
import HistoryView from "@/views/HistoryView.vue";
import LoginView from "@/views/LoginView.vue";
import AdminUsersView from "@/views/AdminUsersView.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/login",
      name: "login",
      component: LoginView
    },
    {
      path: "/",
      name: "generator",
      component: GeneratorView
    },
    {
      path: "/history",
      name: "history",
      component: HistoryView
    },
    {
      path: "/admin/users",
      name: "admin-users",
      component: AdminUsersView,
      meta: {
        requiresAdmin: true
      }
    }
  ]
});

router.beforeEach((to) => {
  const token = localStorage.getItem("uigen_auth_token");
  const role = readUserRole();
  if (!token && to.path !== "/login") {
    return "/login";
  }
  if (token && to.path === "/login") {
    return "/";
  }
  if (to.meta?.requiresAdmin && role !== "ADMIN") {
    return "/";
  }
  return true;
});

function readUserRole() {
  try {
    const raw = localStorage.getItem("uigen_auth_user");
    if (!raw) {
      return "";
    }
    const user = JSON.parse(raw);
    return user?.role || "";
  } catch (_) {
    return "";
  }
}

export default router;
