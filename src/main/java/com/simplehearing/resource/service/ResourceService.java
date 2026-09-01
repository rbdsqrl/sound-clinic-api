package com.simplehearing.resource.service;

import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.resource.dto.*;
import com.simplehearing.resource.entity.Resource;
import com.simplehearing.resource.entity.ResourceFolder;
import com.simplehearing.resource.repository.ResourceFolderRepository;
import com.simplehearing.resource.repository.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ResourceService {

    private final ResourceFolderRepository folderRepository;
    private final ResourceRepository resourceRepository;

    public ResourceService(ResourceFolderRepository folderRepository, ResourceRepository resourceRepository) {
        this.folderRepository = folderRepository;
        this.resourceRepository = resourceRepository;
    }

    /** Everything a folder-browsing screen needs — the folder, its breadcrumb, and its
     *  immediate children. Pass folderId=null for the root. */
    @Transactional(readOnly = true)
    public ResourceFolderContentsResponse getContents(UUID orgId, UUID folderId) {
        ResourceFolder folder = null;
        List<ResourceFolderResponse> breadcrumb = new LinkedList<>();

        if (folderId != null) {
            folder = requireFolder(orgId, folderId);

            LinkedList<ResourceFolderResponse> trail = new LinkedList<>();
            UUID ancestorId = folder.getParentFolderId();
            while (ancestorId != null) {
                ResourceFolder ancestor = folderRepository.findById(ancestorId).orElse(null);
                if (ancestor == null) break;
                trail.addFirst(toResponse(ancestor));
                ancestorId = ancestor.getParentFolderId();
            }
            breadcrumb = trail;
        }

        List<ResourceFolder> subfolders = folderId == null
                ? folderRepository.findByOrgIdAndParentFolderIdIsNullOrderByNameAsc(orgId)
                : folderRepository.findByOrgIdAndParentFolderIdOrderByNameAsc(orgId, folderId);

        List<Resource> resources = folderId == null
                ? resourceRepository.findByOrgIdAndFolderIdIsNullOrderByNameAsc(orgId)
                : resourceRepository.findByOrgIdAndFolderIdOrderByNameAsc(orgId, folderId);

        return new ResourceFolderContentsResponse(
                folder != null ? toResponse(folder) : null,
                breadcrumb,
                subfolders.stream().map(this::toResponse).toList(),
                resources.stream().map(ResourceResponse::from).toList()
        );
    }

    public ResourceFolderResponse createFolder(UUID orgId, UUID createdBy, CreateResourceFolderRequest request) {
        if (request.parentFolderId() != null) {
            requireFolder(orgId, request.parentFolderId());
        }

        ResourceFolder folder = new ResourceFolder();
        folder.setOrgId(orgId);
        folder.setParentFolderId(request.parentFolderId());
        folder.setName(request.name().trim());
        folder.setCreatedBy(createdBy);

        return toResponse(folderRepository.save(folder));
    }

    public ResourceFolderResponse renameFolder(UUID orgId, UUID id, UpdateResourceFolderRequest request) {
        ResourceFolder folder = requireFolder(orgId, id);
        folder.setName(request.name().trim());
        return toResponse(folderRepository.save(folder));
    }

    /** Deleting a folder cascades to its subfolders and resources (DB-level ON DELETE CASCADE). */
    public void deleteFolder(UUID orgId, UUID id) {
        ResourceFolder folder = requireFolder(orgId, id);
        folderRepository.delete(folder);
    }

    public ResourceResponse createResource(UUID orgId, UUID createdBy, CreateResourceRequest request) {
        if (request.folderId() != null) {
            requireFolder(orgId, request.folderId());
        }

        Resource resource = new Resource();
        resource.setOrgId(orgId);
        resource.setFolderId(request.folderId());
        resource.setName(request.name().trim());
        resource.setType(request.type());
        resource.setUrl(request.url().trim());
        resource.setCreatedBy(createdBy);

        return ResourceResponse.from(resourceRepository.save(resource));
    }

    public ResourceResponse updateResource(UUID orgId, UUID id, UpdateResourceRequest request) {
        Resource resource = requireResource(orgId, id);
        resource.setName(request.name().trim());
        resource.setType(request.type());
        resource.setUrl(request.url().trim());
        return ResourceResponse.from(resourceRepository.save(resource));
    }

    public void deleteResource(UUID orgId, UUID id) {
        resourceRepository.delete(requireResource(orgId, id));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private ResourceFolder requireFolder(UUID orgId, UUID id) {
        return folderRepository.findById(id)
                .filter(f -> f.getOrgId().equals(orgId))
                .orElseThrow(() -> new ResourceNotFoundException("Folder", id));
    }

    private Resource requireResource(UUID orgId, UUID id) {
        return resourceRepository.findById(id)
                .filter(r -> r.getOrgId().equals(orgId))
                .orElseThrow(() -> new ResourceNotFoundException("Resource", id));
    }

    private ResourceFolderResponse toResponse(ResourceFolder f) {
        long subCount = folderRepository.countByOrgIdAndParentFolderId(f.getOrgId(), f.getId());
        long resCount = resourceRepository.countByOrgIdAndFolderId(f.getOrgId(), f.getId());
        return ResourceFolderResponse.from(f, subCount, resCount);
    }
}
