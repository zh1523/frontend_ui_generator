import { apiRequest } from "@/api/client";

export function listAdminUsers({ keyword = "", page = 0, size = 20 } = {}) {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size)
  });
  if (keyword?.trim()) {
    params.set("keyword", keyword.trim());
  }
  return apiRequest(`/admin/users?${params.toString()}`);
}

export function updateAdminUserRole(userId, role) {
  return apiRequest(`/admin/users/${userId}/role`, {
    method: "PATCH",
    body: { role }
  });
}

export function updateAdminUserStatus(userId, enabled) {
  return apiRequest(`/admin/users/${userId}/status`, {
    method: "PATCH",
    body: { enabled }
  });
}

export function listAdminAudits({ page = 0, size = 20 } = {}) {
  return apiRequest(`/admin/audits?page=${page}&size=${size}`);
}
