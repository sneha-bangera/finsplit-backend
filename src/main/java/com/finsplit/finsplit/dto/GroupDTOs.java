package com.finsplit.finsplit.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class GroupDTOs {

    // ── Requests ──────────────────────────────────────────

    public record CreateGroupRequest(
            @NotBlank(message = "Group name is required")
            String name,
            String description
    ) {}

    public record AddMemberRequest(
            @NotBlank(message = "Email is required")
            String email
    ) {}

    public record AddGroupExpenseRequest(
            @NotNull(message = "Amount is required")
            @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
            BigDecimal amount,

            @NotBlank(message = "Description is required")
            String description,

            @NotBlank(message = "Date is required")
            String date,

            // "EQUAL" or "CUSTOM"
            @NotBlank(message = "Split type is required")
            String splitType,

            // Only required when splitType = "CUSTOM"
            // Key = userId, Value = share amount
            Map<String, BigDecimal> customSplits
    ) {}

    // ── Responses ─────────────────────────────────────────

    public record MemberResponse(
            String id,
            String name,
            String email
    ) {}

    public record GroupResponse(
            String id,
            String name,
            String description,
            MemberResponse createdBy,
            List<MemberResponse> members,
            int expenseCount,
            String createdAt
    ) {}

    public record SplitResponse(
            String userId,
            String userName,
            BigDecimal shareAmount,
            boolean settled
    ) {}

    public record GroupExpenseResponse(
            String id,
            String description,
            BigDecimal amount,
            String date,
            String splitType,
            MemberResponse paidBy,
            List<SplitResponse> splits,
            String createdAt
    ) {}

    public record BalanceEntry(
            String userId,
            String userName,
            BigDecimal netBalance  // positive = owed money, negative = owes money
    ) {}

    public record SettlementSuggestion(
            String fromUserId,
            String fromUserName,
            String toUserId,
            String toUserName,
            BigDecimal amount
    ) {}

    public record GroupBalanceResponse(
            List<BalanceEntry> balances,
            List<SettlementSuggestion> suggestions
    ) {}
}