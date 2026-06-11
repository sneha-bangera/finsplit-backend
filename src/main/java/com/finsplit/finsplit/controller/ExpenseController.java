package com.finsplit.finsplit.controller;

import com.finsplit.finsplit.dto.ExpenseDTOs.*;
import com.finsplit.finsplit.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<ExpenseResponse> create(
            @AuthenticationPrincipal UserDetails u,
            @Valid @RequestBody CreateExpenseRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(expenseService.create(u.getUsername(), req));
    }

    @GetMapping
    public ResponseEntity<List<ExpenseResponse>> getAll(
            @AuthenticationPrincipal UserDetails u,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(expenseService.getFiltered(u.getUsername(), from, to, categoryId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponse> update(
            @AuthenticationPrincipal UserDetails u,
            @PathVariable String id,
            @Valid @RequestBody UpdateExpenseRequest req) {
        return ResponseEntity.ok(expenseService.update(u.getUsername(), id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails u,
            @PathVariable String id) {
        expenseService.delete(u.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/summary")
    public ResponseEntity<ExpenseSummaryResponse> summary(
            @AuthenticationPrincipal UserDetails u) {
        return ResponseEntity.ok(expenseService.getSummary(u.getUsername()));
    }
}
