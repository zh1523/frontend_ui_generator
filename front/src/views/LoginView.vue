<template>
  <div class="login-page">
    <el-card class="login-card">
      <template #header>
        <div class="title">UI 生成平台登录</div>
      </template>
      <el-tabs v-model="mode">
        <el-tab-pane label="登录" name="login" />
        <el-tab-pane label="注册" name="register" />
      </el-tabs>
      <el-form label-position="top" @submit.prevent="handleSubmit">
        <el-form-item label="用户名">
          <el-input v-model="form.username" maxlength="32" placeholder="输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" show-password maxlength="64" placeholder="输入密码" />
        </el-form-item>
        <el-button type="primary" class="submit-btn" :loading="loading" @click="handleSubmit">
          {{ mode === "login" ? "登录" : "注册并登录" }}
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import { useProjectStore } from "@/stores/project";

const router = useRouter();
const authStore = useAuthStore();
const projectStore = useProjectStore();
const loading = ref(false);
const mode = ref("login");
const form = reactive({
  username: "",
  password: ""
});

async function handleSubmit() {
  if (!form.username || !form.password) {
    ElMessage.warning("请输入用户名和密码");
    return;
  }
  loading.value = true;
  try {
    if (mode.value === "login") {
      await authStore.login({
        username: form.username,
        password: form.password
      });
    } else {
      await authStore.register({
        username: form.username,
        password: form.password
      });
    }
    await projectStore.ensureProject();
    ElMessage.success("登录成功");
    await router.replace("/");
  } catch (error) {
    ElMessage.error(error.message || "操作失败");
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.login-page {
  min-height: calc(100vh - 160px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-card {
  width: 100%;
  max-width: 420px;
}

.title {
  font-size: 18px;
  font-weight: 700;
}

.submit-btn {
  width: 100%;
}
</style>
