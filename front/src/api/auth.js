import { apiRequest } from "@/api/client";

export function register(payload) {
  return apiRequest("/auth/register", {
    method: "POST",
    body: payload
  });
}

export function login(payload) {
  return apiRequest("/auth/login", {
    method: "POST",
    body: payload
  });
}

export function logout() {
  return apiRequest("/auth/logout", {
    method: "POST"
  });
}

export function me() {
  return apiRequest("/auth/me");
}
