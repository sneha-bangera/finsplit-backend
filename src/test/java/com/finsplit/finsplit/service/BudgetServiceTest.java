package com.finsplit.finsplit.service;

import com.finsplit.finsplit.dto.BudgetDTOs.*;
import com.finsplit.finsplit.entity.*;
import com.finsplit.finsplit.exception.ResourceNotFoundException;
import com.finsplit.finsplit.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BudgetService Tests")
class BudgetServiceTest {

    @Mock private BudgetRepository   budgetRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private UserRepository     userRepository;
    @Mock private ExpenseRepository  expenseRepository;
    @Mock private EmailService       emailService;

    @InjectMocks
    private BudgetService budgetService;

    private User     mockUser;
    private Category mockCategory;
    private String   currentMonth;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .name("Rahul Shah")
                .email("rahul@example.com")
                .password("encoded")
                .build();

        mockCategory = Category.builder()
                .id(1L).name("Food & Dining").icon("food").build();

        currentMonth = YearMonth.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    // ── Set budget tests ──────────────────────────────────

    @Test
    @DisplayName("SetBudget: creates new budget when none exists")
    void setBudget_createsNew() {
        when(userRepository.findByEmail("rahul@example.com"))
                .thenReturn(Optional.of(mockUser));
        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(mockCategory));
        when(budgetRepository
                .findByUserAndCategory_IdAndMonthYear(mockUser, 1L, currentMonth))
                .thenReturn(Optional.empty());

        Budget savedBudget = Budget.builder()
                .id(UUID.randomUUID())
                .user(mockUser)
                .category(mockCategory)
                .monthYear(currentMonth)
                .limitAmount(new BigDecimal("5000.00"))
                .build();
        when(budgetRepository.save(any(Budget.class))).thenReturn(savedBudget);

        // No expenses this month
        when(expenseRepository.findByUserAndCategory_IdOrderByDateDesc(any(), any()))
                .thenReturn(List.of());

        BudgetStatusResponse response = budgetService.setBudget(
                "rahul@example.com",
                new SetBudgetRequest(1L, currentMonth, new BigDecimal("5000.00"))
        );

        assertThat(response.limitAmount()).isEqualByComparingTo("5000.00");
        assertThat(response.spentAmount()).isEqualByComparingTo("0.00");
        assertThat(response.exceeded()).isFalse();
        assertThat(response.percentageUsed()).isEqualTo(0.0);

        verify(budgetRepository).save(any(Budget.class));
    }

    @Test
    @DisplayName("SetBudget: updates existing budget (upsert)")
    void setBudget_updatesExisting() {
        Budget existingBudget = Budget.builder()
                .id(UUID.randomUUID())
                .user(mockUser)
                .category(mockCategory)
                .monthYear(currentMonth)
                .limitAmount(new BigDecimal("3000.00"))
                .build();

        when(userRepository.findByEmail("rahul@example.com"))
                .thenReturn(Optional.of(mockUser));
        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(mockCategory));
        when(budgetRepository
                .findByUserAndCategory_IdAndMonthYear(mockUser, 1L, currentMonth))
                .thenReturn(Optional.of(existingBudget));
        when(budgetRepository.save(any())).thenReturn(existingBudget);
        when(expenseRepository.findByUserAndCategory_IdOrderByDateDesc(any(), any()))
                .thenReturn(List.of());

        budgetService.setBudget(
                "rahul@example.com",
                new SetBudgetRequest(1L, currentMonth, new BigDecimal("8000.00"))
        );

        // Confirm the limit was updated on the existing object
        assertThat(existingBudget.getLimitAmount())
                .isEqualByComparingTo("8000.00");
        verify(budgetRepository).save(existingBudget);
    }

    @Test
    @DisplayName("SetBudget: percentage used is correct when partially spent")
    void setBudget_percentageCalculation() {
        when(userRepository.findByEmail(any()))
                .thenReturn(Optional.of(mockUser));
        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(mockCategory));
        when(budgetRepository
                .findByUserAndCategory_IdAndMonthYear(any(), any(), any()))
                .thenReturn(Optional.empty());

        Budget saved = Budget.builder()
                .id(UUID.randomUUID()).user(mockUser)
                .category(mockCategory).monthYear(currentMonth)
                .limitAmount(new BigDecimal("1000.00")).build();
        when(budgetRepository.save(any())).thenReturn(saved);

        // Spent ₹750 out of ₹1000 limit
        Expense spent = Expense.builder()
                .id(UUID.randomUUID()).user(mockUser).category(mockCategory)
                .amount(new BigDecimal("750.00"))
                .date(LocalDate.now()).build();
        when(expenseRepository.findByUserAndCategory_IdOrderByDateDesc(any(), any()))
                .thenReturn(List.of(spent));

        BudgetStatusResponse response = budgetService.setBudget(
                "rahul@example.com",
                new SetBudgetRequest(1L, currentMonth, new BigDecimal("1000.00"))
        );

        assertThat(response.spentAmount()).isEqualByComparingTo("750.00");
        assertThat(response.percentageUsed()).isEqualTo(75.0);
        assertThat(response.exceeded()).isFalse();
    }

    @Test
    @DisplayName("SetBudget: exceeded is true when spent >= limit")
    void setBudget_exceededWhenOverBudget() {
        when(userRepository.findByEmail(any()))
                .thenReturn(Optional.of(mockUser));
        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(mockCategory));
        when(budgetRepository
                .findByUserAndCategory_IdAndMonthYear(any(), any(), any()))
                .thenReturn(Optional.empty());

        Budget saved = Budget.builder()
                .id(UUID.randomUUID()).user(mockUser)
                .category(mockCategory).monthYear(currentMonth)
                .limitAmount(new BigDecimal("500.00")).build();
        when(budgetRepository.save(any())).thenReturn(saved);

        // Spent ₹600 — over the ₹500 limit
        Expense over = Expense.builder()
                .id(UUID.randomUUID()).user(mockUser).category(mockCategory)
                .amount(new BigDecimal("600.00"))
                .date(LocalDate.now()).build();
        when(expenseRepository.findByUserAndCategory_IdOrderByDateDesc(any(), any()))
                .thenReturn(List.of(over));

        BudgetStatusResponse response = budgetService.setBudget(
                "rahul@example.com",
                new SetBudgetRequest(1L, currentMonth, new BigDecimal("500.00"))
        );

        assertThat(response.exceeded()).isTrue();
        assertThat(response.percentageUsed()).isGreaterThan(100.0);
        // Email alert should be triggered
        verify(emailService).sendBudgetAlert(
                any(), any(), any(), anyDouble(), anyDouble(), anyDouble(), any());
    }

    @Test
    @DisplayName("SetBudget: fails when category not found")
    void setBudget_categoryNotFound() {
        when(userRepository.findByEmail(any()))
                .thenReturn(Optional.of(mockUser));
        when(categoryRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                budgetService.setBudget("rahul@example.com",
                        new SetBudgetRequest(99L, currentMonth, new BigDecimal("1000.00")))
        ).isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found");
    }
}
