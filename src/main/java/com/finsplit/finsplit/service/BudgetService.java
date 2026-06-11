package com.finsplit.finsplit.service;

import com.finsplit.finsplit.dto.BudgetDTOs.*;
import com.finsplit.finsplit.entity.Budget;
import com.finsplit.finsplit.entity.User;
import com.finsplit.finsplit.exception.ResourceNotFoundException;
import com.finsplit.finsplit.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository    budgetRepository;
    private final CategoryRepository  categoryRepository;
    private final UserRepository      userRepository;
    private final ExpenseRepository   expenseRepository;
    private final EmailService        emailService;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    // ── helpers ──────────────────────────────────────────

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private BigDecimal getSpent(User user, Long categoryId, String monthYear) {
        YearMonth ym   = YearMonth.parse(monthYear, MONTH_FMT);
        LocalDate from = ym.atDay(1);
        LocalDate to   = ym.atEndOfMonth();

        return expenseRepository
                .findByUserAndCategory_IdOrderByDateDesc(user, categoryId)
                .stream()
                .filter(e -> !e.getDate().isBefore(from) && !e.getDate().isAfter(to))
                .map(e -> e.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BudgetStatusResponse toStatus(Budget b) {
        BigDecimal spent     = getSpent(b.getUser(), b.getCategory().getId(), b.getMonthYear());
        BigDecimal remaining = b.getLimitAmount().subtract(spent);
        double pct = b.getLimitAmount().compareTo(BigDecimal.ZERO) == 0 ? 0 :
                spent.divide(b.getLimitAmount(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();

        return new BudgetStatusResponse(
                b.getId().toString(),
                b.getCategory().getName(),
                b.getCategory().getIcon(),
                b.getLimitAmount(),
                spent,
                remaining,
                Math.round(pct * 10.0) / 10.0,
                spent.compareTo(b.getLimitAmount()) >= 0,
                b.getMonthYear()
        );
    }

    // ── Set or update a budget ────────────────────────────

    public BudgetStatusResponse setBudget(String email, SetBudgetRequest req) {
        var user     = getUser(email);
        var category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        // Upsert — update if exists, create if not
        var budget = budgetRepository
                .findByUserAndCategory_IdAndMonthYear(user, req.categoryId(), req.monthYear())
                .orElse(Budget.builder().user(user).category(category)
                        .monthYear(req.monthYear()).build());

        budget.setLimitAmount(req.limitAmount());
        var saved  = budgetRepository.save(budget);
        var status = toStatus(saved);

        // Send alert immediately if already over budget when setting
        if (status.exceeded()) {
            emailService.sendBudgetAlert(
                    email, user.getName(), category.getName(),
                    status.percentageUsed(),
                    status.spentAmount().doubleValue(),
                    status.limitAmount().doubleValue(),
                    req.monthYear()
            );
        }

        return status;
    }

    // ── Get all budgets for a month ───────────────────────

    public List<BudgetStatusResponse> getBudgetsForMonth(String email, String monthYear) {
        var user = getUser(email);
        String month = (monthYear != null) ? monthYear
                : YearMonth.now().format(MONTH_FMT);

        return budgetRepository.findByUserAndMonthYear(user, month)
                .stream().map(this::toStatus).toList();
    }

    // ── Analytics — last 6 months totals ─────────────────

    public AnalyticsSummaryResponse getAnalytics(String email) {
        var user   = getUser(email);
        String now = YearMonth.now().format(MONTH_FMT);

        List<BudgetStatusResponse> budgets = getBudgetsForMonth(email, now);

        // Build last 6 months spend totals
        List<MonthlyTotalResponse> last6 = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym   = YearMonth.now().minusMonths(i);
            LocalDate from = ym.atDay(1);
            LocalDate to   = ym.atEndOfMonth();
            BigDecimal total = expenseRepository
                    .sumByUserAndDateBetween(user, from, to);
            last6.add(new MonthlyTotalResponse(ym.format(MONTH_FMT), total));
        }

        return new AnalyticsSummaryResponse(budgets, last6);
    }

    // ── Scheduled weekly reminder ─────────────────────────
    // Runs every Monday at 9 AM — checks all current-month budgets
    // and emails users who have exceeded 80%

    public void checkAndAlertBudgets(String email) {
        String month = YearMonth.now().format(MONTH_FMT);
        var user     = getUser(email);
        var budgets  = budgetRepository.findByUserAndMonthYear(user, month);

        for (Budget b : budgets) {
            var status = toStatus(b);
            if (status.percentageUsed() >= 80) {
                emailService.sendBudgetAlert(
                        email, user.getName(), b.getCategory().getName(),
                        status.percentageUsed(),
                        status.spentAmount().doubleValue(),
                        status.limitAmount().doubleValue(),
                        month
                );
            }
        }
    }
}