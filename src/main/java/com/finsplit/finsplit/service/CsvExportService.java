package com.finsplit.finsplit.service;

import com.finsplit.finsplit.entity.Expense;
import com.finsplit.finsplit.entity.User;
import com.finsplit.finsplit.exception.ResourceNotFoundException;
import com.finsplit.finsplit.repository.ExpenseRepository;
import com.finsplit.finsplit.repository.UserRepository;
import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CsvExportService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository    userRepository;

    public byte[] exportExpenses(String email, String from, String to) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Expense> expenses;

        if (from != null && to != null) {
            expenses = expenseRepository.findByUserAndDateBetweenOrderByDateDesc(
                    user, LocalDate.parse(from), LocalDate.parse(to));
        } else {
            expenses = expenseRepository.findByUserOrderByDateDesc(user);
        }

        try (StringWriter sw = new StringWriter();
             CSVWriter writer = new CSVWriter(sw)) {

            // Header row
            writer.writeNext(new String[]{
                    "Date", "Category", "Amount (INR)", "Note"
            });

            // Data rows
            for (Expense e : expenses) {
                writer.writeNext(new String[]{
                        e.getDate().toString(),
                        e.getCategory().getName(),
                        e.getAmount().toPlainString(),
                        e.getNote() != null ? e.getNote() : ""
                });
            }

            return sw.toString().getBytes();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CSV");
        }
    }
}