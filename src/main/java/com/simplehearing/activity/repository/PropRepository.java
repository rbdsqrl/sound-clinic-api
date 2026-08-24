package com.simplehearing.activity.repository;

import com.simplehearing.activity.entity.Prop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PropRepository extends JpaRepository<Prop, UUID> {
    List<Prop> findByOrgIdAndIsActiveTrueOrderByNameAsc(UUID orgId);
    Optional<Prop> findByOrgIdAndNameIgnoreCase(UUID orgId, String name);
}
