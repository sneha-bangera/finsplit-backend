package com.finsplit.finsplit.service;

import com.finsplit.finsplit.dto.ExpenseDTOs.*;
import com.finsplit.finsplit.entity.Expense;
import com.finsplit.finsplit.entity.User;
import com.finsplit.finsplit.exception.ResourceNotFoundException;
import com.finsplit.finsplit.exception.UnauthorizedException;
import com.finsplit.finsplit.repository.CategoryRepository;
import com.finsplit.finsplit.repository.ExpenseRepository;
import com.finsplit.finsplit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    // ── helpers ──────────────────────────────────────────

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private ExpenseResponse toResponse(Expense e) {
        String receiptUrl = e.getReceiptPath() != null
                ? "/api/expenses/" + e.getId() + "/receipt"
                : null;
        return new ExpenseResponse(
                e.getId().toString(),
                e.getAmount(),
                e.getDate().toString(),
                e.getNote(),
                new CategoryResponse(e.getCategory().getId(),
                        e.getCategory().getName(),
                        e.getCategory().getIcon()),
                receiptUrl,
                e.getCreatedAt().toString()
        );
    }

    // ── CRUD ─────────────────────────────────────────────

    public ExpenseResponse create(String email, CreateExpenseRequest req) {
        var user = getUser(email);
        var category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        var expense = Expense.builder()
                .user(user)
                .category(category)
                .amount(req.amount())
                .date(LocalDate.parse(req.date()))
                .note(req.note())
                .build();

        return toResponse(expenseRepository.save(expense));
    }

    public List<ExpenseResponse> getAll(String email) {
        return expenseRepository.findByUserOrderByDateDesc(getUser(email))
                .stream().map(this::toResponse).toList();
    }

    public List<ExpenseResponse> getFiltered(String email, String from, String to, Long categoryId) {
        var user = getUser(email);

        if (categoryId != null) {
            return expenseRepository
                    .findByUserAndCategory_IdOrderByDateDesc(user, categoryId)
                    .stream().map(this::toResponse).toList();
        }

        // If no date range provided, return ALL expenses — no filter
        if (from == null && to == null) {
            return expenseRepository.findByUserOrderByDateDesc(user)
                    .stream().map(this::toResponse).toList();
        }

        LocalDate f = LocalDate.parse(from);
        LocalDate t = LocalDate.parse(to);
        return expenseRepository.findByUserAndDateBetweenOrderByDateDesc(user, f, t)
                .stream().map(this::toResponse).toList();

//        LocalDate f = from != null ? LocalDate.parse(from) : LocalDate.now().minusMonths(1);
//        LocalDate t = to   != null ? LocalDate.parse(to)   : LocalDate.now();
//        return expenseRepository.findByUserAndDateBetweenOrderByDateDesc(user, f, t)
//                .stream().map(this::toResponse).toList();
    }

    public ExpenseResponse update(String email, String expenseId, UpdateExpenseRequest req) {
        var expense = expenseRepository.findById(UUID.fromString(expenseId))
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));

        if (!expense.getUser().getEmail().equals(email))
            throw new UnauthorizedException("You can only edit your own expenses");

        var category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        expense.setAmount(req.amount());
        expense.setCategory(category);
        expense.setDate(LocalDate.parse(req.date()));
        expense.setNote(req.note());

        return toResponse(expenseRepository.save(expense));
    }

    public void delete(String email, String expenseId) {
        var expense = expenseRepository.findById(UUID.fromString(expenseId))
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));

        if (!expense.getUser().getEmail().equals(email))
            throw new UnauthorizedException("You can only delete your own expenses");

        // Delete receipt file from disk if exists
        fileStorageService.delete(expense.getReceiptPath());

        expenseRepository.delete(expense);
    }

    // ── Summary / analytics ──────────────────────────────

    public ExpenseSummaryResponse getSummary(String email) {
        var user = getUser(email);
        YearMonth current = YearMonth.now();
        LocalDate from = current.atDay(1);
        LocalDate to   = current.atEndOfMonth();

        BigDecimal total = expenseRepository.sumByUserAndDateBetween(user, from, to);
        List<Expense> thisMonth = expenseRepository
                .findByUserAndDateBetweenOrderByDateDesc(user, from, to);

        List<CategoryBreakdown> breakdown = expenseRepository
                .sumByCategoryForUser(user, from, to)
                .stream()
                .map(row -> new CategoryBreakdown((String) row[0], (BigDecimal) row[1]))
                .toList();

        List<ExpenseResponse> recent = expenseRepository
                .findByUserOrderByDateDesc(user)
                .stream().limit(5).map(this::toResponse).toList();

        return new ExpenseSummaryResponse(total, thisMonth.size(), breakdown, recent);
    }

    public ExpenseResponse uploadReceipt(String email, String expenseId, MultipartFile file) {
        var expense = expenseRepository.findById(UUID.fromString(expenseId))
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));

        if (!expense.getUser().getEmail().equals(email))
            throw new UnauthorizedException("You can only upload receipts for your own expenses");

        // Delete old receipt if replacing
        fileStorageService.delete(expense.getReceiptPath());

        String path = fileStorageService.store(file, expense.getUser().getId().toString());
        expense.setReceiptPath(path);

        return toResponse(expenseRepository.save(expense));
    }

    public byte[] getReceipt(String email, String expenseId) {
        var expense = expenseRepository.findById(UUID.fromString(expenseId))
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));

        if (!expense.getUser().getEmail().equals(email))
            throw new UnauthorizedException("Access denied");

        if (expense.getReceiptPath() == null)
            throw new ResourceNotFoundException("No receipt for this expense");

        return fileStorageService.load(expense.getReceiptPath());
    }
}
