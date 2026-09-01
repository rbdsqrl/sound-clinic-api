package com.simplehearing.resource.dto;

import java.util.List;

/** Everything a folder-browsing screen needs in one call — the folder itself (null at
 *  root), the breadcrumb trail up to root, and its immediate subfolders/resources. */
public record ResourceFolderContentsResponse(
        ResourceFolderResponse folder,
        List<ResourceFolderResponse> breadcrumb,
        List<ResourceFolderResponse> subfolders,
        List<ResourceResponse> resources
) {}
