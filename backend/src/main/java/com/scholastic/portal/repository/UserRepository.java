package com.scholastic.portal.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scholastic.portal.model.Role;
import com.scholastic.portal.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    List<User> findByRole(Role role);

    boolean existsByUsername(String username);
}