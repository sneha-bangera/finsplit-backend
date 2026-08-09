package com.finsplit.finsplit.service;

import com.finsplit.finsplit.dto.ExpenseDTOs.*;
import com.finsplit.finsplit.entity.Category;
import com.finsplit.finsplit.entity.Expense;
import com.finsplit.finsplit.entity.User;
import com.finsplit.finsplit.exception.ResourceNotFoundException;
import com.finsplit.finsplit.exception.UnauthorizedException;
import com.finsplit.finsplit.repository.CategoryRepository;
import com.finsplit.finsplit.repository.ExpenseRepository;
import com.finsplit.finsplit.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseService Tests")
class ExpenseServiceTest {

    @Mock private ExpenseRepository  expenseRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private UserRepository     userRepository;
    @Mock private FileStorageService fileStorageService;

    @InjectMocks
    private ExpenseService expenseService;

    private User     mockUser;
    private Category mockCategory;
    private Expense  mockExpense;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .name("Rahul Shah")
                .email("rahul@example.com")
                .password("encoded")
                .build();

        mockCategory = Category.builder()
                .id(1L)
                .name("Food & Dining")
                .icon("food")
                .build();

        mockExpense = Expense.builder()
                .id(UUID.randomUUID())
                .user(mockUser)
                .category(mockCategory)
                .amount(new BigDecimal("500.00"))
                .date(LocalDate.now())
                .note("Lunch")
                .build();

        // Set createdAt via reflection since it's @CreationTimestamp
        try {
            var field = Expense.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(mockExpense, LocalDateTime.now());
        } catch (Exception ignored) {}
    }

    // ── Create expense tests ──────────────────────────────

    @Test
    @DisplayName("Create: success — returns expense response with correct values")
    void createExpense_success() {
        when(userRepository.findByEmail("rahul@example.com"))
                .thenReturn(Optional.of(mockUser));
        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(mockCategory));
        when(expenseRepository.save(any(Expense.class)))
                .thenReturn(mockExpense);

        ExpenseResponse response = expenseService.create(
                "rahul@example.com",
                new CreateExpenseRequest(
                        new BigDecimal("500.00"), 1L,
                        LocalDate.now().toString(), "Lunch")
        );

        assertThat(response.amount()).isEqualByComparingTo("500.00");
        assertThat(response.note()).isEqualTo("Lunch");
        assertThat(response.category().name()).isEqualTo("Food & Dining");
    }

    @Test
    @DisplayName("Create: fails when category not found")
    void createExpense_categoryNotFound() {
        when(userRepository.findByEmail(any()))
                .thenReturn(Optional.of(mockUser));
        when(categoryRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                expenseService.create("rahul@example.com",
                        new CreateExpenseRequest(
                                new BigDecimal("500.00"), 99L,
                                LocalDate.now().toString(), "Test"))
        ).isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    @DisplayName("Create: fails when user not found")
    void createExpense_userNotFound() {
        when(userRepository.findByEmail("ghost@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                expenseService.create("ghost@example.com",
                        new CreateExpenseRequest(
                                new BigDecimal("100.00"), 1L,
                                LocalDate.now().toString(), null))
        ).isInstanceOf(ResourceNotFoundException.class);
    }

    // ── Delete expense tests ──────────────────────────────

    @Test
    @DisplayName("Delete: success — expense is deleted when user owns it")
    void deleteExpense_success() {
        when(expenseRepository.findById(mockExpense.getId()))
                .thenReturn(Optional.of(mockExpense));

        expenseService.delete("rahul@example.com", mockExpense.getId().toString());

        verify(expenseRepository).delete(mockExpense);
    }

    @Test
    @DisplayName("Delete: fails when user does not own the expense")
    void deleteExpense_notOwner_throwsUnauthorized() {
        when(expenseRepository.findById(mockExpense.getId()))
                .thenReturn(Optional.of(mockExpense));

        assertThatThrownBy(() ->
                expenseService.delete("other@example.com",
                        mockExpense.getId().toString())
        ).isInstanceOf(UnauthorizedException.class);

        verify(expenseRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Delete: fails when expense does not exist")
    void deleteExpense_notFound() {
        UUID randomId = UUID.randomUUID();
        when(expenseRepository.findById(randomId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                expenseService.delete("rahul@example.com", randomId.toString())
        ).isInstanceOf(ResourceNotFoundException.class);
    }

    // ── Get all expenses tests ────────────────────────────

    @Test
    @DisplayName("GetAll: returns all expenses for user")
    void getAll_returnsExpenses() {
        when(userRepository.findByEmail("rahul@example.com"))
                .thenReturn(Optional.of(mockUser));
        when(expenseRepository.findByUserOrderByDateDesc(mockUser))
                .thenReturn(List.of(mockExpense));

        List<ExpenseResponse> result = expenseService.getFiltered(
                "rahul@example.com", null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).amount())
                .isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("GetAll: returns empty list when user has no expenses")
    void getAll_emptyList() {
        when(userRepository.findByEmail("rahul@example.com"))
                .thenReturn(Optional.of(mockUser));
        when(expenseRepository.findByUserOrderByDateDesc(mockUser))
                .thenReturn(List.of());

        List<ExpenseResponse> result = expenseService.getFiltered(
                "rahul@example.com", null, null, null);

        assertThat(result).isEmpty();
    }
}
