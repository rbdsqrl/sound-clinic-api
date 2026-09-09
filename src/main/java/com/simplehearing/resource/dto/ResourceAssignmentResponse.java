package com.simplehearing.resource.dto;

import com.simplehearing.resource.entity.ResourceAssignment;
import com.simplehearing.resource.enums.ResourceType;

import java.time.Instant;
import java.util.UUID;

/** A Resources-library item as assigned to one patient — carries the resource's own display
 *  fields alongside the assignment's own id/metadata, so a caller (staff caseload view, or the
 *  Parent app's own filtered list) never needs a second lookup to render it. */
public record ResourceAssignmentResponse(
        UUID id,
        UUID resourceId,
        String resourceName,
        ResourceType resourceType,
        String resourceUrl,
        boolean hosted,
        UUID patientId,
        UUID assignedBy,
        String assignedByName,
        Instant createdAt
) {
    public static ResourceAssignmentResponse from(ResourceAssignment a, String name, ResourceType type,
                                                   String resolvedUrl, boolean hosted, String assignedByName) {
        return new ResourceAssignmentResponse(
                a.getId(), a.getResourceId(), name, type, resolvedUrl, hosted,
                a.getPatientId(), a.getAssignedBy(), assignedByName, a.getCreatedAt());
    }
}
