package com.simplehearing.task.dto;

import com.simplehearing.task.entity.TaskLog;
import com.simplehearing.task.enums.TaskLogType;

import java.time.Instant;
import java.util.UUID;

public record TaskLogResponse(
        UUID id,
        UUID taskId,
        TaskLogType logType,
        UUID actorId,
        String actorName,
        String details,
        Instant createdAt
) {
    public static TaskLogResponse from(TaskLog log) {
        return new TaskLogResponse(
                log.getId(), log.getTaskId(), log.getLogType(),
                log.getActorId(), log.getActorName(), log.getDetails(), log.getCreatedAt()
        );
    }
}
