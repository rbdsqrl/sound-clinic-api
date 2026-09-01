package com.simplehearing.resource.dto;

import java.util.List;

public record ImportResourcesResponse(int foldersCreated, int resourcesCreated, List<String> errors) {}
