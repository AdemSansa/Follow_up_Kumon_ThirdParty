package com.school.meetings.repository;

import com.school.followup.model.FollowUpStatus;
import com.school.meetings.model.OnlineMeeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OnlineMeetingRepository extends JpaRepository<OnlineMeeting, UUID> {

    Optional<OnlineMeeting> findByFollowUpId(UUID followUpId);

    List<OnlineMeeting> findByFollowUpIdIn(Collection<UUID> followUpIds);

    /** Open online follow-ups starting in (now, until] that still need a Meet link (and haven't used up their retries). */
    @Query("""
            select f.id from FollowUp f
            where f.type = com.school.followup.model.MeetingType.VIDEO_CALL
              and f.status in :statuses
              and f.scheduledStart > :now and f.scheduledStart <= :until
              and not exists (
                  select m.id from OnlineMeeting m
                  where m.followUp = f
                    and (m.status in (com.school.meetings.model.MeetingStatus.CREATED,
                                      com.school.meetings.model.MeetingStatus.CANCELLED)
                         or m.attempts >= :maxAttempts))
            order by f.scheduledStart""")
    List<UUID> findFollowUpsNeedingMeeting(@Param("statuses") Collection<FollowUpStatus> statuses,
                                           @Param("now") LocalDateTime now,
                                           @Param("until") LocalDateTime until,
                                           @Param("maxAttempts") int maxAttempts);

    /** Open online follow-ups starting in (now, until] with a link, whose parent invite for that time hasn't gone out. */
    @Query("""
            select f.id from OnlineMeeting m join m.followUp f
            where m.status = com.school.meetings.model.MeetingStatus.CREATED and m.joinUrl is not null
              and f.type = com.school.followup.model.MeetingType.VIDEO_CALL
              and f.status in :statuses
              and f.scheduledStart > :now and f.scheduledStart <= :until
              and not exists (
                  select n.id from Notification n
                  where n.followUp = f and n.scheduledFor = f.scheduledStart and n.status = 'SENT'
                    and n.kind in (com.school.meetings.model.NotificationKind.MEETING_INVITE,
                                   com.school.meetings.model.NotificationKind.MEETING_RESCHEDULED))
            order by f.scheduledStart""")
    List<UUID> findFollowUpsNeedingInvite(@Param("statuses") Collection<FollowUpStatus> statuses,
                                          @Param("now") LocalDateTime now,
                                          @Param("until") LocalDateTime until);
}
