package com.example.uigen.common;

import com.example.uigen.model.entity.AppUser;

public final class AuthContextHolder {

    private static final ThreadLocal<AppUser> USER_HOLDER = new ThreadLocal<>();

    private AuthContextHolder() {
    }

    public static void setUser(AppUser user) {
        USER_HOLDER.set(user);
    }

    public static AppUser getUser() {
        return USER_HOLDER.get();
    }

    public static Long getUserId() {
        AppUser user = USER_HOLDER.get();
        return user == null ? null : user.getId();
    }

    public static void clear() {
        USER_HOLDER.remove();
    }
}
