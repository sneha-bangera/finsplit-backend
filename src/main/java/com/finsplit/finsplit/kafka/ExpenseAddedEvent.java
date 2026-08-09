package com.finsplit.finsplit.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseAddedEvent {

    private String        eventId;       // unique event ID
    private String        groupId;
    private String        groupName;
    private String        expenseId;
    private String        description;
    private BigDecimal    amount;
    private String        splitType;
    private String        paidByUserId;
    private String        paidByName;
    private String        paidByEmail;
    private LocalDateTime occurredAt;
}