package com.simplehearing.task.dto;

import com.simplehearing.task.entity.TaskComment;

import java.time.Instant;
import java.util.UUID;

public record TaskCommentResponse(
        UUID id,
        UUID taskId,
        UUID authorId,
        String authorFirstName,
        String authorLastName,
        String body,
        Instant createdAt
) {
    public static TaskCommentResponse from(TaskComment c, String firstName, String lastName) {
        return new TaskCommentResponse(
                c.getId(), c.getTaskId(), c.getAuthorId(),
                firstName, lastName, c.getBody(), c.getCreatedAt());
    }
}
