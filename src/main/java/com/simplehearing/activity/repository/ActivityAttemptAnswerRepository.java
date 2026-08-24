package com.simplehearing.activity.repository;

import com.simplehearing.activity.entity.ActivityAttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActivityAttemptAnswerRepository extends JpaRepository<ActivityAttemptAnswer, UUID> {
    List<ActivityAttemptAnswer> findByAttemptLogId(UUID attemptLogId);

    @org.springframework.data.jpa.repository.Query(
            "SELECT a FROM ActivityAttemptAnswer a WHERE a.attemptLogId IN :attemptLogIds")
    List<ActivityAttemptAnswer> findByAttemptLogIdIn(
            @org.springframework.data.repository.query.Param("attemptLogIds") List<UUID> attemptLogIds);
}
