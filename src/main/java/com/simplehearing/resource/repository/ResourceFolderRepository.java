package com.simplehearing.resource.repository;

import com.simplehearing.resource.entity.ResourceFolder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ResourceFolderRepository extends JpaRepository<ResourceFolder, UUID> {

    List<ResourceFolder> findByOrgIdAndParentFolderIdIsNullOrderByNameAsc(UUID orgId);

    List<ResourceFolder> findByOrgIdAndParentFolderIdOrderByNameAsc(UUID orgId, UUID parentFolderId);

    List<ResourceFolder> findByOrgIdAndParentFolderId(UUID orgId, UUID parentFolderId);

    long countByOrgIdAndParentFolderId(UUID orgId, UUID parentFolderId);

    java.util.Optional<ResourceFolder> findByOrgIdAndParentFolderIdIsNullAndName(UUID orgId, String name);

    java.util.Optional<ResourceFolder> findByOrgIdAndParentFolderIdAndName(UUID orgId, UUID parentFolderId, String name);
}
