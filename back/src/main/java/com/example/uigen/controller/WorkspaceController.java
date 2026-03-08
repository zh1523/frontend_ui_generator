package com.example.uigen.controller;

import com.example.uigen.model.dto.CreateWorkspaceResponse;
import com.example.uigen.service.WorkspaceService;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping("/anonymous")
    public CreateWorkspaceResponse createAnonymous(HttpServletRequest request) {
        String ipHash = sha256Hex(request.getRemoteAddr());
        String uaHash = sha256Hex(request.getHeader("User-Agent"));
        return workspaceService.createAnonymousWorkspace(ipHash, uaHash);
    }

    private String sha256Hex(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }
}
