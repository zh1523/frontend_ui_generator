package com.example.uigen.controller;

import com.example.uigen.common.HttpHeadersConst;
import com.example.uigen.model.dto.AuthLoginRequest;
import com.example.uigen.model.dto.AuthRegisterRequest;
import com.example.uigen.model.dto.AuthResponse;
import com.example.uigen.model.dto.UserProfileResponse;
import com.example.uigen.model.entity.AppUser;
import com.example.uigen.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody AuthRegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthLoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    public void logout(@RequestHeader(value = HttpHeadersConst.AUTH_TOKEN, required = false) String token) {
        authService.logout(token);
    }

    @GetMapping("/me")
    public UserProfileResponse me(@RequestHeader(HttpHeadersConst.AUTH_TOKEN) String token) {
        AppUser user = authService.requireUserByToken(token);
        return authService.toProfile(user);
    }
}
