package com.simplehearing.resource.service;

import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.resource.dto.*;
import com.simplehearing.resource.entity.Resource;
import com.simplehearing.resource.entity.ResourceFolder;
import com.simplehearing.resource.enums.ResourceType;
import com.simplehearing.resource.repository.ResourceFolderRepository;
import com.simplehearing.resource.repository.ResourceRepository;
import com.simplehearing.storage.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Service
@Transactional
public class ResourceService {

    private final ResourceFolderRepository folderRepository;
    private final ResourceRepository resourceRepository;
    private final StorageService storageService;

    public ResourceService(ResourceFolderRepository folderRepository, ResourceRepository resourceRepository, StorageService storageService) {
        this.storageService = storageService;
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
                resources.stream().map(this::toResponse).toList()
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

        return toResponse(resourceRepository.save(resource));
    }

    public ResourceResponse updateResource(UUID orgId, UUID id, UpdateResourceRequest request) {
        Resource resource = requireResource(orgId, id);
        resource.setName(request.name().trim());
        resource.setType(request.type());
        resource.setUrl(request.url().trim());
        return toResponse(resourceRepository.save(resource));
    }

    public void deleteResource(UUID orgId, UUID id) {
        resourceRepository.delete(requireResource(orgId, id));
    }

    /** Stores an uploaded file and returns its URL — used to fill a resource's `url` field
     *  as an alternative to pasting an external link, for any resource type. */
    public String uploadFile(UUID orgId, MultipartFile file) {
        try {
            return storageService.store(file, "resources/" + orgId);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file");
        }
    }

    /**
     * Bulk-imports resources from a CSV with columns folder_path,name,type,url — folder_path
     * is "/"-separated (e.g. "Alphabet/Tracing"), empty for a root-level resource. Missing
     * folders along a path are created and reused across rows in the same import; an existing
     * folder with a matching name at that level is reused rather than duplicated.
     * Not exposed in any UI — org-scoped, callable directly against whichever environment/org
     * the caller is authenticated against.
     */
    public ImportResourcesResponse importCsv(UUID orgId, UUID createdBy, MultipartFile file) {
        List<String> lines;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            lines = reader.lines().toList();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Failed to read CSV file");
        }
        if (lines.isEmpty()) {
            return new ImportResourcesResponse(0, 0, List.of());
        }

        int foldersCreated = 0;
        int resourcesCreated = 0;
        List<String> errors = new ArrayList<>();
        Map<String, UUID> pathCache = new HashMap<>();

        for (int rowNum = 2; rowNum <= lines.size(); rowNum++) {
            String line = lines.get(rowNum - 1);
            if (line.isBlank()) continue;

            List<String> fields = parseCsvLine(line);
            if (fields.size() != 4) {
                errors.add("Row " + rowNum + ": expected 4 columns, got " + fields.size());
                continue;
            }
            String folderPath = fields.get(0).trim();
            String name = fields.get(1).trim();
            String typeStr = fields.get(2).trim();
            String url = fields.get(3).trim();

            if (name.isEmpty() || url.isEmpty()) {
                errors.add("Row " + rowNum + ": name and url are required");
                continue;
            }
            ResourceType type;
            try {
                type = ResourceType.valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                errors.add("Row " + rowNum + ": invalid type '" + typeStr + "'");
                continue;
            }

            UUID folderId = null;
            if (!folderPath.isEmpty()) {
                UUID parentId = null;
                StringBuilder pathSoFar = new StringBuilder();
                for (String rawSegment : folderPath.split("/")) {
                    String segment = rawSegment.trim();
                    if (segment.isEmpty()) continue;
                    pathSoFar.append(pathSoFar.isEmpty() ? "" : "/").append(segment);
                    String key = pathSoFar.toString();

                    UUID cached = pathCache.get(key);
                    if (cached != null) {
                        parentId = cached;
                        continue;
                    }

                    UUID finalParentId = parentId;
                    Optional<ResourceFolder> existing = parentId == null
                            ? folderRepository.findByOrgIdAndParentFolderIdIsNullAndName(orgId, segment)
                            : folderRepository.findByOrgIdAndParentFolderIdAndName(orgId, finalParentId, segment);

                    UUID resolvedId;
                    if (existing.isPresent()) {
                        resolvedId = existing.get().getId();
                    } else {
                        ResourceFolder folder = new ResourceFolder();
                        folder.setOrgId(orgId);
                        folder.setParentFolderId(parentId);
                        folder.setName(segment);
                        folder.setCreatedBy(createdBy);
                        resolvedId = folderRepository.save(folder).getId();
                        foldersCreated++;
                    }
                    pathCache.put(key, resolvedId);
                    parentId = resolvedId;
                }
                folderId = parentId;
            }

            Resource resource = new Resource();
            resource.setOrgId(orgId);
            resource.setFolderId(folderId);
            resource.setName(name);
            resource.setType(type);
            resource.setUrl(url);
            resource.setCreatedBy(createdBy);
            resourceRepository.save(resource);
            resourcesCreated++;
        }

        return new ImportResourcesResponse(foldersCreated, resourcesCreated, errors);
    }

    /** Minimal CSV line parser — handles double-quoted fields with "" as an escaped quote. */
    private List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
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

    /** Re-signs the stored URL on every read — a pasted external link (YouTube, Google Drive)
     *  passes through presign() unchanged; a file we stored gets a fresh time-limited link
     *  instead of the permanent one baked in at upload time. Matches Task/SharedMedia/Feed. */
    private ResourceResponse toResponse(Resource r) {
        return ResourceResponse.from(r, storageService.presign(r.getUrl(), Duration.ofHours(1)));
    }
}
