package com.finsplit.finsplit.controller;

import com.finsplit.finsplit.dto.BudgetDTOs.*;
import com.finsplit.finsplit.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    // Set or update a budget
    @PostMapping
    public ResponseEntity<BudgetStatusResponse> setBudget(
            @AuthenticationPrincipal UserDetails u,
            @Valid @RequestBody SetBudgetRequest req) {
        return ResponseEntity.ok(budgetService.setBudget(u.getUsername(), req));
    }

    // Get all budgets for a given month (default: current month)
    @GetMapping
    public ResponseEntity<List<BudgetStatusResponse>> getBudgets(
            @AuthenticationPrincipal UserDetails u,
            @RequestParam(required = false) String monthYear) {
        return ResponseEntity.ok(budgetService.getBudgetsForMonth(u.getUsername(), monthYear));
    }

    // Full analytics — budget statuses + last 6 months chart data
    @GetMapping("/analytics")
    public ResponseEntity<AnalyticsSummaryResponse> getAnalytics(
            @AuthenticationPrincipal UserDetails u) {
        return ResponseEntity.ok(budgetService.getAnalytics(u.getUsername()));
    }
}