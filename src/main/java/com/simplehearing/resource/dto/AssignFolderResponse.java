package com.simplehearing.resource.dto;

/** Summary of a whole-folder assignment — every resource in the folder's subtree (subfolders
 *  included) is assigned to the patient in one call; resources already assigned to that patient
 *  are counted separately rather than erroring, since a bulk operation shouldn't fail outright
 *  over one item that was assigned earlier. */
public record AssignFolderResponse(
        int totalResources,
        int assignedCount,
        int alreadyAssignedCount
) {}
