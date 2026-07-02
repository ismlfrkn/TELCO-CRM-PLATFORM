package com.turkcell.identity.repository;

import com.turkcell.identity.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByIdAndStatusNot(UUID id, String status);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Page<User> findAllByStatusNot(String status, Pageable pageable);
}
