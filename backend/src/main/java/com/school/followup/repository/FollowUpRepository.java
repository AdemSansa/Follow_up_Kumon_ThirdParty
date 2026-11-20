package com.school.followup.repository;

import com.school.followup.model.FollowUp;
import com.school.followup.model.FollowUpStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FollowUpRepository extends JpaRepository<FollowUp, UUID>, JpaSpecificationExecutor<FollowUp> {

    List<FollowUp> findByFollowUpPlanIdOrderByScheduledStartAsc(UUID followUpPlanId);

    boolean existsByFollowUpPlanId(UUID followUpPlanId);

    boolean existsByStudentId(UUID studentId);

    List<FollowUp> findByScheduledStartGreaterThanEqualAndScheduledStartLessThanOrderByScheduledStartAsc(
            LocalDateTime fromInclusive, LocalDateTime toExclusive);

    long countByFollowUpPlanIdAndScheduledStartLessThanEqual(UUID followUpPlanId, LocalDateTime start);

    Optional<FollowUp> findFirstByStudentIdAndStatusAndScheduledStartBeforeOrderByScheduledStartDesc(
            UUID studentId, FollowUpStatus status, LocalDateTime before);

    List<FollowUp> findByFollowUpPlanIdAndStatusAndScheduledStartGreaterThanEqual(
            UUID followUpPlanId, FollowUpStatus status, LocalDateTime from);
}
