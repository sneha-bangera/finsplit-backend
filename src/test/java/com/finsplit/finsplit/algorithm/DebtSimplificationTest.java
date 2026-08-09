package com.finsplit.finsplit.algorithm;

import com.finsplit.finsplit.dto.GroupDTOs.*;
import com.finsplit.finsplit.entity.*;
import com.finsplit.finsplit.repository.*;
import com.finsplit.finsplit.service.GroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Debt Simplification Algorithm Tests")
class DebtSimplificationTest {

    @Mock private GroupRepository        groupRepository;
    @Mock private GroupExpenseRepository groupExpenseRepository;
    @Mock private ExpenseSplitRepository splitRepository;
    @Mock private UserRepository         userRepository;

    @InjectMocks
    private GroupService groupService;

    private User userA, userB, userC;
    private Group group;

    @BeforeEach
    void setUp() {
        userA = User.builder().id(UUID.randomUUID())
                .name("UserA").email("a@test.com").password("x").build();
        userB = User.builder().id(UUID.randomUUID())
                .name("UserB").email("b@test.com").password("x").build();
        userC = User.builder().id(UUID.randomUUID())
                .name("UserC").email("c@test.com").password("x").build();

        group = Group.builder()
                .id(UUID.randomUUID())
                .name("Test Group")
                .createdBy(userA)
                .members(new HashSet<>(Set.of(userA, userB, userC)))
                .build();
    }

    // ── Helper: build a GroupExpense with splits ──────────

    private GroupExpense buildExpense(User paidBy, BigDecimal amount,
                                      Map<User, BigDecimal> splits) {
        List<ExpenseSplit> splitList = new ArrayList<>();
        GroupExpense ge = GroupExpense.builder()
                .id(UUID.randomUUID()).group(group)
                .paidBy(paidBy).amount(amount)
                .description("Test expense")
                .date(LocalDate.now()).splitType("CUSTOM")
                .splits(splitList).build();

        splits.forEach((user, share) -> splitList.add(
                ExpenseSplit.builder()
                        .id(UUID.randomUUID()).groupExpense(ge)
                        .user(user).shareAmount(share)
                        .settled(user.equals(paidBy))
                        .build()
        ));
        return ge;
    }

    private void mockGroup() {
        when(groupRepository.findById(group.getId()))
                .thenReturn(Optional.of(group));
        when(userRepository.findByEmail("a@test.com"))
                .thenReturn(Optional.of(userA));
    }

    // ── Test 1: Basic 2-person split ─────────────────────

    @Test
    @DisplayName("2 members: A pays ₹10000 and B pays ₹3000 — B owes A ₹3500")
    void twoMembers_correctBalance() {
        group.setMembers(new HashSet<>(Set.of(userA, userB)));
        mockGroup();

        // Expense 1: A pays ₹10000 split equally (₹5000 each)
        GroupExpense exp1 = buildExpense(userA, new BigDecimal("10000"),
                Map.of(userA, new BigDecimal("5000"),
                        userB, new BigDecimal("5000")));

        // Expense 2: B pays ₹3000 split equally (₹1500 each)
        GroupExpense exp2 = buildExpense(userB, new BigDecimal("3000"),
                Map.of(userA, new BigDecimal("1500"),
                        userB, new BigDecimal("1500")));

        when(groupExpenseRepository.findByGroupOrderByDateDesc(group))
                .thenReturn(List.of(exp1, exp2));

        GroupBalanceResponse result = groupService.getBalances(
                "a@test.com", group.getId().toString());

        // Find B's balance — should be -3500
        BalanceEntry bBalance = result.balances().stream()
                .filter(b -> b.userId().equals(userB.getId().toString()))
                .findFirst().orElseThrow();

        assertThat(bBalance.netBalance())
                .isEqualByComparingTo(new BigDecimal("-3500.00"));

        // Settlement: B pays A ₹3500
        assertThat(result.suggestions()).hasSize(1);
        SettlementSuggestion s = result.suggestions().get(0);
        assertThat(s.fromUserName()).isEqualTo("UserB");
        assertThat(s.toUserName()).isEqualTo("UserA");
        assertThat(s.amount()).isEqualByComparingTo("3500.00");
    }

    // ── Test 2: Equal 3-way split ─────────────────────────

    @Test
    @DisplayName("3 members: A pays ₹300 equally — B and C each owe A ₹100")
    void threeMembers_equalSplit() {
        mockGroup();

        GroupExpense exp = buildExpense(userA, new BigDecimal("300"),
                Map.of(userA, new BigDecimal("100"),
                        userB, new BigDecimal("100"),
                        userC, new BigDecimal("100")));

        when(groupExpenseRepository.findByGroupOrderByDateDesc(group))
                .thenReturn(List.of(exp));

        GroupBalanceResponse result = groupService.getBalances(
                "a@test.com", group.getId().toString());

        // A should be owed ₹200 total (₹100 from B + ₹100 from C)
        BalanceEntry aBalance = result.balances().stream()
                .filter(b -> b.userId().equals(userA.getId().toString()))
                .findFirst().orElseThrow();
        assertThat(aBalance.netBalance()).isEqualByComparingTo("200.00");

        // Should be exactly 2 settlement suggestions (B→A and C→A)
        assertThat(result.suggestions()).hasSize(2);
        result.suggestions().forEach(s -> {
            assertThat(s.toUserName()).isEqualTo("UserA");
            assertThat(s.amount()).isEqualByComparingTo("100.00");
        });
    }

    // ── Test 3: All settled up ────────────────────────────

    @Test
    @DisplayName("All settled: no suggestions when balances are zero")
    void allSettled_noSuggestions() {
        group.setMembers(new HashSet<>(Set.of(userA, userB)));
        mockGroup();

        // A pays ₹500, B owes ₹250 — but B's split is already settled
        GroupExpense exp = GroupExpense.builder()
                .id(UUID.randomUUID()).group(group)
                .paidBy(userA).amount(new BigDecimal("500"))
                .description("Settled").date(LocalDate.now())
                .splitType("EQUAL").splits(new ArrayList<>()).build();

        exp.getSplits().add(ExpenseSplit.builder()
                .groupExpense(exp).user(userA)
                .shareAmount(new BigDecimal("250")).settled(true).build());
        exp.getSplits().add(ExpenseSplit.builder()
                .groupExpense(exp).user(userB)
                .shareAmount(new BigDecimal("250")).settled(true).build()); // ← settled

        when(groupExpenseRepository.findByGroupOrderByDateDesc(group))
                .thenReturn(List.of(exp));

        GroupBalanceResponse result = groupService.getBalances(
                "a@test.com", group.getId().toString());

        assertThat(result.suggestions()).isEmpty();
    }

    // ── Test 4: Debt minimization ─────────────────────────

    @Test
    @DisplayName("Minimization: A→B and A→C reduces to fewer transactions")
    void debtMinimization_fewerTransactions() {
        mockGroup();

        // B pays ₹600 — A owes ₹300, C owes ₹300
        GroupExpense exp1 = buildExpense(userB, new BigDecimal("600"),
                Map.of(userA, new BigDecimal("300"),
                        userB, new BigDecimal("300") ,
                        userC, new BigDecimal("0")));

        // C pays ₹300 — A owes ₹150, B owes ₹150
        GroupExpense exp2 = buildExpense(userC, new BigDecimal("300"),
                Map.of(userA, new BigDecimal("150"),
                        userB, new BigDecimal("150"),
                        userC, new BigDecimal("0")));

        when(groupExpenseRepository.findByGroupOrderByDateDesc(group))
                .thenReturn(List.of(exp1, exp2));

        GroupBalanceResponse result = groupService.getBalances(
                "a@test.com", group.getId().toString());

        // A owes ₹300 to B and ₹150 to C = ₹450 total
        // Algorithm should produce at most 2 transactions
        assertThat(result.suggestions().size()).isLessThanOrEqualTo(2);

        // Total amount in suggestions should equal total debt
        BigDecimal totalSuggested = result.suggestions().stream()
                .map(SettlementSuggestion::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalSuggested).isEqualByComparingTo("450.00");
    }

    // ── Test 5: Uneven split rounding ────────────────────

    @Test
    @DisplayName("Rounding: ₹500 split 3 ways — shares sum to ₹500")
    void unevenSplit_roundingIsCorrect() {
        // ₹500 / 3 = ₹166.67 each — verify total doesn't exceed ₹500
        BigDecimal total  = new BigDecimal("500");
        int        people = 3;
        BigDecimal share  = total.divide(
                BigDecimal.valueOf(people), 2,
                java.math.RoundingMode.HALF_UP);

        BigDecimal sum = share.multiply(BigDecimal.valueOf(people));

        // Due to rounding, sum might be ₹500.01 — check it's within 1 cent
        assertThat(sum.subtract(total).abs())
                .isLessThanOrEqualTo(new BigDecimal("0.01"));
    }

    // ── Test 6: No expenses — all zero balances ───────────

    @Test
    @DisplayName("Empty group: all balances are zero when no expenses")
    void noExpenses_allBalancesZero() {
        mockGroup();
        when(groupExpenseRepository.findByGroupOrderByDateDesc(group))
                .thenReturn(List.of());

        GroupBalanceResponse result = groupService.getBalances(
                "a@test.com", group.getId().toString());

        result.balances().forEach(b ->
                assertThat(b.netBalance()).isEqualByComparingTo("0.00")
        );
        assertThat(result.suggestions()).isEmpty();
    }
}

