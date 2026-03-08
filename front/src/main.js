import { createApp } from "vue";
import { createPinia } from "pinia";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import App from "./App.vue";
import router from "./router";
import "./styles.css";

const savedTheme = localStorage.getItem("uigen_theme_mode");
document.documentElement.setAttribute("data-theme", savedTheme === "dark" ? "dark" : "light");

const app = createApp(App);
app.use(createPinia());
app.use(router);
app.use(ElementPlus);
app.mount("#app");
