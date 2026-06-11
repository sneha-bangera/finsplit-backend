package com.finsplit.finsplit.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class BudgetDTOs {

    public record SetBudgetRequest(
            @NotNull(message = "Category is required")
            Long categoryId,

            @NotBlank(message = "Month is required — format: yyyy-MM")
            String monthYear,

            @NotNull(message = "Limit amount is required")
            @DecimalMin(value = "1.00", message = "Budget must be at least 1")
            BigDecimal limitAmount
    ) {}

    public record BudgetStatusResponse(
            String budgetId,
            String categoryName,
            String categoryIcon,
            BigDecimal limitAmount,
            BigDecimal spentAmount,
            BigDecimal remaining,
            double percentageUsed,
            boolean exceeded,
            String monthYear
    ) {}

    public record MonthlyTotalResponse(
            String monthYear,
            BigDecimal totalSpent
    ) {}

    public record AnalyticsSummaryResponse(
            List<BudgetStatusResponse> budgetStatuses,
            List<MonthlyTotalResponse> last6Months
    ) {}
}