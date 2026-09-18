package com.school.followup.repository;

import com.school.followup.model.FollowUpPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface FollowUpPlanRepository extends JpaRepository<FollowUpPlan, UUID>,
        JpaSpecificationExecutor<FollowUpPlan> {
}
