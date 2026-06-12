package com.finsplit.finsplit.service;

import com.finsplit.finsplit.dto.GroupDTOs.*;
import com.finsplit.finsplit.entity.*;
import com.finsplit.finsplit.exception.ResourceNotFoundException;
import com.finsplit.finsplit.exception.UnauthorizedException;
import com.finsplit.finsplit.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository        groupRepository;
    private final GroupExpenseRepository groupExpenseRepository;
    private final ExpenseSplitRepository splitRepository;
    private final UserRepository         userRepository;

    // ── helpers ──────────────────────────────────────────

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Group getGroup(String groupId) {
        return groupRepository.findById(UUID.fromString(groupId))
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
    }

    private void assertMember(Group group, User user) {
        if (!group.getMembers().contains(user))
            throw new UnauthorizedException("You are not a member of this group");
    }

    private MemberResponse toMember(User u) {
        return new MemberResponse(u.getId().toString(), u.getName(), u.getEmail());
    }

    private GroupResponse toGroupResponse(Group g) {
        int expCount = groupExpenseRepository.findByGroupOrderByDateDesc(g).size();
        return new GroupResponse(
                g.getId().toString(),
                g.getName(),
                g.getDescription(),
                toMember(g.getCreatedBy()),
                g.getMembers().stream().map(this::toMember).toList(),
                expCount,
                g.getCreatedAt().toString()
        );
    }

    private GroupExpenseResponse toExpenseResponse(GroupExpense ge) {
        List<SplitResponse> splits = ge.getSplits().stream()
                .map(s -> new SplitResponse(
                        s.getUser().getId().toString(),
                        s.getUser().getName(),
                        s.getShareAmount(),
                        s.isSettled()
                )).toList();

        return new GroupExpenseResponse(
                ge.getId().toString(),
                ge.getDescription(),
                ge.getAmount(),
                ge.getDate().toString(),
                ge.getSplitType(),
                toMember(ge.getPaidBy()),
                splits,
                ge.getCreatedAt().toString()
        );
    }

    // ── Group CRUD ────────────────────────────────────────

    public GroupResponse createGroup(String email, CreateGroupRequest req) {
        var user  = getUser(email);
        var group = Group.builder()
                .name(req.name())
                .description(req.description())
                .createdBy(user)
                .build();
        group.getMembers().add(user); // creator is auto-member
        return toGroupResponse(groupRepository.save(group));
    }

    public List<GroupResponse> getMyGroups(String email) {
        return groupRepository.findByMembersContaining(getUser(email))
                .stream().map(this::toGroupResponse).toList();
    }

    public GroupResponse getGroup(String email, String groupId) {
        var group = getGroup(groupId);
        assertMember(group, getUser(email));
        return toGroupResponse(group);
    }

    public GroupResponse addMember(String email, String groupId, AddMemberRequest req) {
        var group      = getGroup(groupId);
        var requester  = getUser(email);
        assertMember(group, requester);

        var newMember = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No user found with email: " + req.email() +
                                ". They must register on FinSplit first."));

        if (group.getMembers().contains(newMember))
            throw new IllegalArgumentException("User is already a member of this group");

        group.getMembers().add(newMember);
        return toGroupResponse(groupRepository.save(group));
    }

    public void removeMember(String email, String groupId, String memberId) {
        var group     = getGroup(groupId);
        var requester = getUser(email);

        if (!group.getCreatedBy().equals(requester))
            throw new UnauthorizedException("Only the group creator can remove members");

        var member = userRepository.findById(UUID.fromString(memberId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (group.getCreatedBy().equals(member))
            throw new IllegalArgumentException("Cannot remove the group creator");

        group.getMembers().remove(member);
        groupRepository.save(group);
    }

    // ── Group expenses ────────────────────────────────────

    public GroupExpenseResponse addExpense(String email,
                                           String groupId,
                                           AddGroupExpenseRequest req) {
        var group  = getGroup(groupId);
        var paidBy = getUser(email);
        assertMember(group, paidBy);

        var expense = GroupExpense.builder()
                .group(group)
                .paidBy(paidBy)
                .amount(req.amount())
                .description(req.description())
                .date(LocalDate.parse(req.date()))
                .splitType(req.splitType())
                .build();

        List<ExpenseSplit> splits = new ArrayList<>();

        if ("EQUAL".equals(req.splitType())) {
            List<User> members = new ArrayList<>(group.getMembers());
            BigDecimal share   = req.amount()
                    .divide(BigDecimal.valueOf(members.size()), 2, RoundingMode.HALF_UP);

            for (User member : members) {
                splits.add(ExpenseSplit.builder()
                        .groupExpense(expense)
                        .user(member)
                        .shareAmount(share)
                        .settled(member.equals(paidBy)) // payer's share is pre-settled
                        .build());
            }

        } else if ("CUSTOM".equals(req.splitType())) {
            if (req.customSplits() == null || req.customSplits().isEmpty())
                throw new IllegalArgumentException("Custom splits are required for CUSTOM split type");

            for (Map.Entry<String, BigDecimal> entry : req.customSplits().entrySet()) {
                var member = userRepository.findById(UUID.fromString(entry.getKey()))
                        .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
                splits.add(ExpenseSplit.builder()
                        .groupExpense(expense)
                        .user(member)
                        .shareAmount(entry.getValue())
                        .settled(member.equals(paidBy))
                        .build());
            }
        }

        expense.setSplits(splits);
        return toExpenseResponse(groupExpenseRepository.save(expense));
    }

    public List<GroupExpenseResponse> getGroupExpenses(String email, String groupId) {
        var group = getGroup(groupId);
        assertMember(group, getUser(email));
        return groupExpenseRepository.findByGroupOrderByDateDesc(group)
                .stream().map(this::toExpenseResponse).toList();
    }

    public void deleteExpense(String email, String groupId, String expenseId) {
        var group   = getGroup(groupId);
        var user    = getUser(email);
        assertMember(group, user);

        var expense = groupExpenseRepository
                .findById(UUID.fromString(expenseId))
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));

        if (!expense.getPaidBy().equals(user) && !group.getCreatedBy().equals(user))
            throw new UnauthorizedException("Only the payer or group creator can delete this expense");

        groupExpenseRepository.delete(expense);
    }

    // ── Balances ──────────────────────────────────────────

//    public GroupBalanceResponse getBalances(String email, String groupId) {
//        var group = getGroup(groupId);
//        assertMember(group, getUser(email));
//
//        // net[userId] = total paid - total owed
//        Map<String, BigDecimal> net = new HashMap<>();
//        Map<String, String>     names = new HashMap<>();
//
//        for (User m : group.getMembers()) {
//            net.put(m.getId().toString(), BigDecimal.ZERO);
//            names.put(m.getId().toString(), m.getName());
//        }
//
//        for (GroupExpense ge : groupExpenseRepository.findByGroupOrderByDateDesc(group)) {
//            String payerId = ge.getPaidBy().getId().toString();
//            // payer gets credit for the full amount
//            net.merge(payerId, ge.getAmount(), BigDecimal::add);
//
//            // each member owes their share
//            for (ExpenseSplit split : ge.getSplits()) {
//                if (!split.isSettled()) {
//                    String uid = split.getUser().getId().toString();
//                    net.merge(uid, split.getShareAmount().negate(), BigDecimal::add);
//                }
//            }
//        }
//
//        List<BalanceEntry> balances = net.entrySet().stream()
//                .map(e -> new BalanceEntry(e.getKey(), names.get(e.getKey()), e.getValue()))
//                .toList();
//
//        List<SettlementSuggestion> suggestions = computeSettlements(balances, names);
//
//        return new GroupBalanceResponse(balances, suggestions);
//    }

    public GroupBalanceResponse getBalances(String email, String groupId) {
        var group = getGroup(groupId);
        assertMember(group, getUser(email));

        // net[userId] = what they are owed (positive) or what they owe (negative)
        Map<String, BigDecimal> net   = new HashMap<>();
        Map<String, String>     names = new HashMap<>();

        for (User m : group.getMembers()) {
            net.put(m.getId().toString(), BigDecimal.ZERO);
            names.put(m.getId().toString(), m.getName());
        }

        for (GroupExpense ge : groupExpenseRepository.findByGroupOrderByDateDesc(group)) {
            String payerId = ge.getPaidBy().getId().toString();

            for (ExpenseSplit split : ge.getSplits()) {
                String uid = split.getUser().getId().toString();

                if (split.isSettled()) continue; // skip already settled splits

                if (uid.equals(payerId)) continue; // payer doesn't owe themselves

                // This member owes the payer their share
                // → payer's net goes UP, member's net goes DOWN
                net.merge(payerId, split.getShareAmount(), BigDecimal::add);
                net.merge(uid, split.getShareAmount().negate(), BigDecimal::add);
            }
        }

        List<BalanceEntry> balances = net.entrySet().stream()
                .map(e -> new BalanceEntry(e.getKey(), names.get(e.getKey()), e.getValue()))
                .toList();

        List<SettlementSuggestion> suggestions = computeSettlements(balances, names);
        return new GroupBalanceResponse(balances, suggestions);
    }

    private List<SettlementSuggestion> computeSettlements(
            List<BalanceEntry> balances, Map<String, String> names) {

        // creditors = people who are owed money (positive net)
        // debtors   = people who owe money (negative net)
        Map<String, BigDecimal> credits = new HashMap<>();
        Map<String, BigDecimal> debts   = new HashMap<>();

        for (BalanceEntry b : balances) {
            int cmp = b.netBalance().compareTo(BigDecimal.ZERO);
            if (cmp > 0) credits.put(b.userId(), b.netBalance());
            if (cmp < 0) debts.put(b.userId(), b.netBalance().negate()); // store as positive
        }

        List<SettlementSuggestion> suggestions = new ArrayList<>();

        // Work on mutable copies
        Map<String, BigDecimal> creditMap = new HashMap<>(credits);
        Map<String, BigDecimal> debtMap   = new HashMap<>(debts);

        List<String> creditorIds = new ArrayList<>(creditMap.keySet());
        List<String> debtorIds   = new ArrayList<>(debtMap.keySet());

        // Sort largest first for cleaner suggestions
        creditorIds.sort((a, b) -> creditMap.get(b).compareTo(creditMap.get(a)));
        debtorIds.sort((a, b)   -> debtMap.get(b).compareTo(debtMap.get(a)));

        int ci = 0, di = 0;
        while (ci < creditorIds.size() && di < debtorIds.size()) {
            String     cId     = creditorIds.get(ci);
            String     dId     = debtorIds.get(di);
            BigDecimal credit  = creditMap.get(cId);
            BigDecimal debt    = debtMap.get(dId);
            BigDecimal payment = credit.min(debt);

            if (payment.compareTo(BigDecimal.ZERO) > 0) {
                suggestions.add(new SettlementSuggestion(
                        dId, names.get(dId),
                        cId, names.get(cId),
                        payment.setScale(2, RoundingMode.HALF_UP)
                ));
            }

            creditMap.put(cId, credit.subtract(payment));
            debtMap.put(dId, debt.subtract(payment));

            if (creditMap.get(cId).compareTo(BigDecimal.ZERO) == 0) ci++;
            if (debtMap.get(dId).compareTo(BigDecimal.ZERO) == 0) di++;
        }

        return suggestions;
    }

    public void markSettled(String email, String groupId, String splitId) {
        var group = getGroup(groupId);
        assertMember(group, getUser(email));

        var split = splitRepository.findById(UUID.fromString(splitId))
                .orElseThrow(() -> new ResourceNotFoundException("Split not found"));

        split.setSettled(true);
        splitRepository.save(split);
    }

    // ── Debt simplification algorithm ─────────────────────
    // Greedy approach: sort creditors and debtors, match largest pairs first
    // Result: minimum number of transactions to settle all debts

//    private List<SettlementSuggestion> computeSettlements(
//            List<BalanceEntry> balances, Map<String, String> names) {
//
//        // Separate into who owes (negative) and who is owed (positive)
//        List<BalanceEntry> creditors = balances.stream()
//                .filter(b -> b.netBalance().compareTo(BigDecimal.ZERO) > 0)
//                .sorted(Comparator.comparing(BalanceEntry::netBalance).reversed())
//                .collect(Collectors.toCollection(ArrayList::new));
//
//        List<BalanceEntry> debtors = balances.stream()
//                .filter(b -> b.netBalance().compareTo(BigDecimal.ZERO) < 0)
//                .sorted(Comparator.comparing(BalanceEntry::netBalance))
//                .collect(Collectors.toCollection(ArrayList::new));
//
//        List<SettlementSuggestion> suggestions = new ArrayList<>();
//
//        // Use mutable amounts for the algorithm
//        Map<String, BigDecimal> creditMap = new HashMap<>();
//        Map<String, BigDecimal> debtMap   = new HashMap<>();
//
//        creditors.forEach(c -> creditMap.put(c.userId(), c.netBalance()));
//        debtors.forEach(d   -> debtMap.put(d.userId(),
//                d.netBalance().negate())); // store as positive
//
//        List<String> creditorIds = new ArrayList<>(creditMap.keySet());
//        List<String> debtorIds   = new ArrayList<>(debtMap.keySet());
//
//        int ci = 0, di = 0;
//        while (ci < creditorIds.size() && di < debtorIds.size()) {
//            String cId  = creditorIds.get(ci);
//            String dId  = debtorIds.get(di);
//            BigDecimal credit = creditMap.get(cId);
//            BigDecimal debt   = debtMap.get(dId);
//
//            BigDecimal payment = credit.min(debt);
//
//            suggestions.add(new SettlementSuggestion(
//                    dId, names.get(dId),
//                    cId, names.get(cId),
//                    payment.setScale(2, RoundingMode.HALF_UP)
//            ));
//
//            creditMap.put(cId, credit.subtract(payment));
//            debtMap.put(dId, debt.subtract(payment));
//
//            if (creditMap.get(cId).compareTo(BigDecimal.ZERO) == 0) ci++;
//            if (debtMap.get(dId).compareTo(BigDecimal.ZERO) == 0) di++;
//        }
//
//        return suggestions;
//    }
}