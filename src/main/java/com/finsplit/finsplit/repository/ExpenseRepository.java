package com.finsplit.finsplit.repository;

import com.finsplit.finsplit.entity.Expense;
import com.finsplit.finsplit.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID>{

    List<Expense> findByUserOrderByDateDesc(User user);

    List<Expense> findByUserAndDateBetweenOrderByDateDesc(
            User user, LocalDate from, LocalDate to);

    List<Expense> findByUserAndCategory_IdOrderByDateDesc(User user, Long categoryId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
            "WHERE e.user = :user AND e.date BETWEEN :from AND :to")
    BigDecimal sumByUserAndDateBetween(
            @Param("user") User user,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("SELECT e.category.name, SUM(e.amount) FROM Expense e " +
            "WHERE e.user = :user AND e.date BETWEEN :from AND :to " +
            "GROUP BY e.category.name ORDER BY SUM(e.amount) DESC")
    List<Object[]> sumByCategoryForUser(
            @Param("user") User user,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
