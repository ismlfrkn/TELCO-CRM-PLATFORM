package com.turkcell.identity.repository;

import com.turkcell.identity.entity.UserRole;
import com.turkcell.identity.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    @Query("SELECT ur FROM UserRole ur WHERE ur.id.userId = :userId")
    List<UserRole> findAllByUserId(@Param("userId") UUID userId);

    @Query("SELECT ur FROM UserRole ur WHERE ur.id.roleId = :roleId")
    List<UserRole> findAllByRoleId(@Param("roleId") UUID roleId);
}
