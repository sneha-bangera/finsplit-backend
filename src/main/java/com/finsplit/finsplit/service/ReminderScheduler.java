package com.finsplit.finsplit.service;

import com.finsplit.finsplit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderScheduler {

    private final BudgetService  budgetService;
    private final UserRepository userRepository;

    // Every Monday at 9:00 AM
    @Scheduled(cron = "0 0 9 * * MON")
    public void weeklyBudgetCheck() {
        log.info("Running weekly budget alert check...");
        userRepository.findAll().forEach(user -> {
            try {
                budgetService.checkAndAlertBudgets(user.getEmail());
            } catch (Exception e) {
                log.error("Budget check failed for {}: {}", user.getEmail(), e.getMessage());
            }
        });
    }
}