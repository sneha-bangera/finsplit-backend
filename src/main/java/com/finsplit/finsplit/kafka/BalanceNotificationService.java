package com.finsplit.finsplit.kafka;

import com.finsplit.finsplit.dto.GroupDTOs.GroupBalanceResponse;
import com.finsplit.finsplit.dto.GroupDTOs.SettlementSuggestion;
import com.finsplit.finsplit.entity.Group;
import com.finsplit.finsplit.repository.GroupRepository;
import com.finsplit.finsplit.service.GroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BalanceNotificationService {

    private final GroupRepository groupRepository;
    private final GroupService    groupService;

    public void recalculateAndNotify(ExpenseAddedEvent event) {
        log.info("═══════════════════════════════════════════════");
        log.info("  BALANCE RECALCULATION TRIGGERED");
        log.info("  Group    : {}", event.getGroupName());
        log.info("  Expense  : {} — ₹{}", event.getDescription(),
                event.getAmount());
        log.info("  Paid by  : {}", event.getPaidByName());
        log.info("  Split    : {}", event.getSplitType());
        log.info("═══════════════════════════════════════════════");

        try {
            // Fetch updated balances using existing GroupService logic
            GroupBalanceResponse balances = groupService.getBalances(
                    event.getPaidByEmail(),
                    event.getGroupId()
            );

            if (balances.suggestions().isEmpty()) {
                log.info("  ✅ All settled up — no pending dues");
            } else {
                log.info("  📊 Updated settlement suggestions:");
                for (SettlementSuggestion s : balances.suggestions()) {
                    log.info("     {} → {} : ₹{}",
                            s.fromUserName(), s.toUserName(), s.amount());
                }
            }

            log.info("  Net balances:");
            balances.balances().forEach(b ->
                    log.info("     {} : ₹{}", b.userName(), b.netBalance())
            );

            // ── Notification stub ─────────────────────────
            // This is where you'd send a push notification,
            // WebSocket message, or email in the future.
            // For now we log it clearly so you can see it working.
            notifyGroupMembers(event, balances);

        } catch (Exception e) {
            log.error("  ❌ Balance recalculation failed: {}", e.getMessage());
            throw e; // rethrow so Kafka retry kicks in
        }

        log.info("═══════════════════════════════════════════════");
    }

    private void notifyGroupMembers(ExpenseAddedEvent event,
                                    GroupBalanceResponse balances) {
        // Stub — in Phase 3+ this becomes a real email or push
        log.info("  🔔 NOTIFICATION STUB");
        log.info("     Would notify all members of group '{}'",
                event.getGroupName());
        log.info("     Message: '{}' added expense '{}' for ₹{}",
                event.getPaidByName(),
                event.getDescription(),
                event.getAmount());

        if (!balances.suggestions().isEmpty()) {
            balances.suggestions().forEach(s ->
                    log.info("     Reminder: {} owes {} ₹{}",
                            s.fromUserName(), s.toUserName(), s.amount())
            );
        }
    }
}