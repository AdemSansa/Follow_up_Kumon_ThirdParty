package com.school.followup.repository;

import com.school.followup.model.FollowUp;
import com.school.followup.model.FollowUpStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface FollowUpRepository extends JpaRepository<FollowUp, UUID>, JpaSpecificationExecutor<FollowUp> {

    List<FollowUp> findByFollowUpPlanIdOrderByScheduledStartAsc(UUID followUpPlanId);

    boolean existsByFollowUpPlanId(UUID followUpPlanId);

    List<FollowUp> findByFollowUpPlanIdAndStatusAndScheduledStartGreaterThanEqual(
            UUID followUpPlanId, FollowUpStatus status, LocalDateTime from);
}
