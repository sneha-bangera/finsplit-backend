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
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

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

    // POST /api/expenses/{id}/receipt — upload a receipt image
    @PostMapping(value = "/{id}/receipt",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExpenseResponse> uploadReceipt(
            @AuthenticationPrincipal UserDetails u,
            @PathVariable String id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(expenseService.uploadReceipt(u.getUsername(), id, file));
    }

    // GET /api/expenses/{id}/receipt — serve the receipt image
    @GetMapping("/{id}/receipt")
    public ResponseEntity<byte[]> getReceipt(
            @AuthenticationPrincipal UserDetails u,
            @PathVariable String id) {
        byte[] image = expenseService.getReceipt(u.getUsername(), id);
        return ResponseEntity.ok()
                .header("Content-Type", "image/jpeg")
                .header("Cache-Control", "max-age=3600")
                .body(image);
    }
}
