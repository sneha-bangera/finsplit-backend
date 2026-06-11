package com.finsplit.finsplit.repository;

import com.finsplit.finsplit.entity.Budget;
import com.finsplit.finsplit.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    List<Budget> findByUserAndMonthYear(User user, String monthYear);

    Optional<Budget> findByUserAndCategory_IdAndMonthYear(
            User user, Long categoryId, String monthYear);
}