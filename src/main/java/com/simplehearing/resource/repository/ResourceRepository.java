package com.simplehearing.resource.repository;

import com.simplehearing.resource.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ResourceRepository extends JpaRepository<Resource, UUID> {

    List<Resource> findByOrgIdAndFolderIdIsNullOrderByNameAsc(UUID orgId);

    List<Resource> findByOrgIdAndFolderIdOrderByNameAsc(UUID orgId, UUID folderId);

    long countByOrgIdAndFolderId(UUID orgId, UUID folderId);
}
