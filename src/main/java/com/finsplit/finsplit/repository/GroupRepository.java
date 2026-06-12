package com.finsplit.finsplit.repository;

import com.finsplit.finsplit.entity.Group;
import com.finsplit.finsplit.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {

    List<Group> findByMembersContaining(User user);
}