import { createRouter, createWebHistory } from "vue-router";
import GeneratorView from "@/views/GeneratorView.vue";
import HistoryView from "@/views/HistoryView.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/",
      name: "generator",
      component: GeneratorView
    },
    {
      path: "/history",
      name: "history",
      component: HistoryView
    }
  ]
});

export default router;
