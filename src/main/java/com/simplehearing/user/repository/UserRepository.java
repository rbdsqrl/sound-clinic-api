package com.simplehearing.user.repository;

import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByClinicIdAndRole(UUID clinicId, Role role);

    List<User> findByClinicId(UUID clinicId);

    List<User> findByOrgIdAndEmailContainingIgnoreCase(UUID orgId, String email);
}
