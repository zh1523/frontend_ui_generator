<template>
  <div class="page">
    <h1 class="page-title">用户权限管理</h1>
    <p class="page-desc">管理员可在此查看账号并调整角色/启用状态。</p>

    <el-card shadow="never" class="section-card">
      <template #header>
        <div class="toolbar">
          <el-input
            v-model="keyword"
            placeholder="按用户名搜索"
            clearable
            class="search-input"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-button type="primary" :loading="loading" @click="handleSearch">查询</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="users" border>
        <el-table-column prop="userId" label="用户ID" min-width="100" />
        <el-table-column prop="username" label="用户名" min-width="160" />
        <el-table-column label="角色" min-width="140">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" effect="plain">
              {{ row.role }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="120">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'warning'" effect="plain">
              {{ row.enabled ? "启用" : "禁用" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近登录" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.lastLoginAt) }}
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="240" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button
                size="small"
                :disabled="isSelf(row)"
                :loading="rowActionLoading[row.userId] === 'role'"
                @click="toggleRole(row)"
              >
                {{ row.role === "ADMIN" ? "设为USER" : "设为ADMIN" }}
              </el-button>
              <el-button
                size="small"
                :type="row.enabled ? 'warning' : 'success'"
                :disabled="isSelf(row) && row.enabled"
                :loading="rowActionLoading[row.userId] === 'status'"
                @click="toggleStatus(row)"
              >
                {{ row.enabled ? "禁用" : "启用" }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :total="total"
          :current-page="page + 1"
          :page-size="size"
          :page-sizes="[10, 20, 50]"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { listAdminUsers, updateAdminUserRole, updateAdminUserStatus } from "@/api/admin";
import { useAuthStore } from "@/stores/auth";

const authStore = useAuthStore();
const loading = ref(false);
const keyword = ref("");
const users = ref([]);
const total = ref(0);
const page = ref(0);
const size = ref(20);
const rowActionLoading = reactive({});

const currentUserId = computed(() => authStore.user?.userId ?? null);

onMounted(async () => {
  await loadUsers();
});

async function loadUsers() {
  loading.value = true;
  try {
    const result = await listAdminUsers({
      keyword: keyword.value,
      page: page.value,
      size: size.value
    });
    users.value = result.items || [];
    total.value = Number(result.total || 0);
    page.value = Number(result.page || 0);
    size.value = Number(result.size || 20);
  } catch (error) {
    ElMessage.error(error.message || "加载用户失败");
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  page.value = 0;
  return loadUsers();
}

function handlePageChange(nextPage) {
  page.value = Math.max(0, Number(nextPage) - 1);
  return loadUsers();
}

function handleSizeChange(nextSize) {
  size.value = Number(nextSize || 20);
  page.value = 0;
  return loadUsers();
}

function isSelf(row) {
  return currentUserId.value != null && Number(row.userId) === Number(currentUserId.value);
}

async function toggleRole(row) {
  const nextRole = row.role === "ADMIN" ? "USER" : "ADMIN";
  try {
    await ElMessageBox.confirm(
      `确认将用户 ${row.username} 的角色改为 ${nextRole} 吗？`,
      "角色变更",
      {
        type: "warning",
        confirmButtonText: "确认",
        cancelButtonText: "取消"
      }
    );
  } catch (_) {
    return;
  }

  rowActionLoading[row.userId] = "role";
  try {
    await updateAdminUserRole(row.userId, nextRole);
    ElMessage.success("角色更新成功");
    await loadUsers();
  } catch (error) {
    ElMessage.error(error.message || "角色更新失败");
  } finally {
    delete rowActionLoading[row.userId];
  }
}

async function toggleStatus(row) {
  const nextEnabled = !row.enabled;
  const actionText = nextEnabled ? "启用" : "禁用";
  try {
    await ElMessageBox.confirm(
      `确认${actionText}用户 ${row.username} 吗？`,
      "状态变更",
      {
        type: "warning",
        confirmButtonText: "确认",
        cancelButtonText: "取消"
      }
    );
  } catch (_) {
    return;
  }

  rowActionLoading[row.userId] = "status";
  try {
    await updateAdminUserStatus(row.userId, nextEnabled);
    ElMessage.success("状态更新成功");
    await loadUsers();
  } catch (error) {
    ElMessage.error(error.message || "状态更新失败");
  } finally {
    delete rowActionLoading[row.userId];
  }
}

function formatDateTime(value) {
  if (!value) {
    return "-";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "-";
  }
  return date.toLocaleString();
}
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
}

.search-input {
  max-width: 320px;
}

.row-actions {
  display: flex;
  gap: 8px;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
