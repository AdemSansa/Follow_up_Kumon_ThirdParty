package com.school.meetings.repository;

import com.school.meetings.model.Notification;
import com.school.meetings.model.NotificationKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Optional<Notification> findByFollowUpIdAndKindAndRecipientAndScheduledFor(
            UUID followUpId, NotificationKind kind, String recipient, LocalDateTime scheduledFor);

    List<Notification> findByFollowUpIdOrderByCreatedAtDesc(UUID followUpId);

    boolean existsByFollowUpIdAndKindAndStatus(UUID followUpId, NotificationKind kind, String status);

    @Query("select distinct n.followUp.id from Notification n where n.followUp.id in :ids")
    List<UUID> findFollowUpIdsWithNotifications(@Param("ids") Collection<UUID> followUpIds);

    /** Latest successful invite/reschedule email for this follow-up's current time, if any. */
    @Query("""
            select max(n.sentAt) from Notification n
            where n.followUp.id = :followUpId and n.scheduledFor = :scheduledFor and n.status = 'SENT'
              and n.kind in (com.school.meetings.model.NotificationKind.MEETING_INVITE,
                             com.school.meetings.model.NotificationKind.MEETING_RESCHEDULED)""")
    java.time.Instant findInviteSentAt(@Param("followUpId") UUID followUpId,
                                       @Param("scheduledFor") LocalDateTime scheduledFor);
}
