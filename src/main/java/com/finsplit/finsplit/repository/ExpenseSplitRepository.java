package com.finsplit.finsplit.repository;

import com.finsplit.finsplit.entity.ExpenseSplit;
import com.finsplit.finsplit.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, UUID> {

    List<ExpenseSplit> findByUserAndSettledFalse(User user);

    List<ExpenseSplit> findByGroupExpense_Group_IdAndSettledFalse(UUID groupId);
}