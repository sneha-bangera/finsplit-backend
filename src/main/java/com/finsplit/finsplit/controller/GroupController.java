package com.finsplit.finsplit.controller;

import com.finsplit.finsplit.dto.GroupDTOs.*;
import com.finsplit.finsplit.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<GroupResponse> create(
            @AuthenticationPrincipal UserDetails u,
            @Valid @RequestBody CreateGroupRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupService.createGroup(u.getUsername(), req));
    }

    @GetMapping
    public ResponseEntity<List<GroupResponse>> getMyGroups(
            @AuthenticationPrincipal UserDetails u) {
        return ResponseEntity.ok(groupService.getMyGroups(u.getUsername()));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupResponse> getGroup(
            @AuthenticationPrincipal UserDetails u,
            @PathVariable String groupId) {
        return ResponseEntity.ok(groupService.getGroup(u.getUsername(), groupId));
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<GroupResponse> addMember(
            @AuthenticationPrincipal UserDetails u,
            @PathVariable String groupId,
            @Valid @RequestBody AddMemberRequest req) {
        return ResponseEntity.ok(groupService.addMember(u.getUsername(), groupId, req));
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @AuthenticationPrincipal UserDetails u,
            @PathVariable String groupId,
            @PathVariable String memberId) {
        groupService.removeMember(u.getUsername(), groupId, memberId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{groupId}/expenses")
    public ResponseEntity<GroupExpenseResponse> addExpense(
            @AuthenticationPrincipal UserDetails u,
            @PathVariable String groupId,
            @Valid @RequestBody AddGroupExpenseRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupService.addExpense(u.getUsername(), groupId, req));
    }

    @GetMapping("/{groupId}/expenses")
    public ResponseEntity<List<GroupExpenseResponse>> getExpenses(
            @AuthenticationPrincipal UserDetails u,
            @PathVariable String groupId) {
        return ResponseEntity.ok(groupService.getGroupExpenses(u.getUsername(), groupId));
    }

    @DeleteMapping("/{groupId}/expenses/{expenseId}")
    public ResponseEntity<Void> deleteExpense(
            @AuthenticationPrincipal UserDetails u,
            @PathVariable String groupId,
            @PathVariable String expenseId) {
        groupService.deleteExpense(u.getUsername(), groupId, expenseId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{groupId}/balances")
    public ResponseEntity<GroupBalanceResponse> getBalances(
            @AuthenticationPrincipal UserDetails u,
            @PathVariable String groupId) {
        return ResponseEntity.ok(groupService.getBalances(u.getUsername(), groupId));
    }

    @PatchMapping("/{groupId}/splits/{splitId}/settle")
    public ResponseEntity<Void> markSettled(
            @AuthenticationPrincipal UserDetails u,
            @PathVariable String groupId,
            @PathVariable String splitId) {
        groupService.markSettled(u.getUsername(), groupId, splitId);
        return ResponseEntity.noContent().build();
    }
}