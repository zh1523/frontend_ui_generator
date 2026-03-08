package com.example.uigen.controller;

import com.example.uigen.common.AuthContextHolder;
import com.example.uigen.model.dto.CostUsageResponse;
import com.example.uigen.service.CostControlService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cost")
public class CostController {

    private final CostControlService costControlService;

    public CostController(CostControlService costControlService) {
        this.costControlService = costControlService;
    }

    @GetMapping("/usage")
    public CostUsageResponse getUsage() {
        Long userId = AuthContextHolder.getUserId();
        return costControlService.getCurrentUsage(userId);
    }
}
