package com.finsplit.finsplit.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public class ExpenseDTOs {

    public record CreateExpenseRequest(
            @NotNull(message = "Amount is required")
            @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
            BigDecimal amount,

            @NotNull(message = "Category is required")
            Long categoryId,

            @NotBlank(message = "Date is required")
            String date,    // "yyyy-MM-dd"

            String note
    ) {}

    public record UpdateExpenseRequest(
            @NotNull(message = "Amount is required")
            @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
            BigDecimal amount,

            @NotNull(message = "Category is required")
            Long categoryId,

            @NotBlank(message = "Date is required")
            String date,

            String note
    ) {}

    public record CategoryResponse(
            Long id,
            String name,
            String icon
    ) {}

    public record ExpenseResponse(
            String id,
            BigDecimal amount,
            String date,
            String note,
            CategoryResponse category,
            String receiptUrl,
            String createdAt
    ) {}

    public record ExpenseSummaryResponse(
            BigDecimal totalThisMonth,
            int countThisMonth,
            List<CategoryBreakdown> categoryBreakdown,
            List<ExpenseResponse> recentExpenses
    ) {}

    public record CategoryBreakdown(
            String category,
            BigDecimal amount
    ) {}
}
