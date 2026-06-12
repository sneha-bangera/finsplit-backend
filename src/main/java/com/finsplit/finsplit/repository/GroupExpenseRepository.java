package com.finsplit.finsplit.repository;

import com.finsplit.finsplit.entity.Group;
import com.finsplit.finsplit.entity.GroupExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GroupExpenseRepository extends JpaRepository<GroupExpense, UUID> {

    List<GroupExpense> findByGroupOrderByDateDesc(Group group);
}