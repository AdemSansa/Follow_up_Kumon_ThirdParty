package com.school.meetings;

import com.school.auth.repository.UserRepository;
import com.school.common.exception.BadRequestException;
import com.school.common.exception.ResourceNotFoundException;
import com.school.followup.model.FollowUp;
import com.school.followup.model.FollowUpStatus;
import com.school.followup.model.MeetingType;
import com.school.followup.repository.FollowUpRepository;
import com.school.meetings.google.GoogleAccount;
import com.school.meetings.google.GoogleAccountRepository;
import com.school.meetings.google.GoogleOAuthService;
import com.school.meetings.model.MeetingStatus;
import com.school.meetings.model.Notification;
import com.school.meetings.model.NotificationKind;
import com.school.meetings.model.OnlineMeeting;
import com.school.meetings.provider.MeetingProvider;
import com.school.meetings.provider.MeetingProvider.CreatedMeeting;
import com.school.meetings.provider.MeetingProvider.MeetingRequest;
import com.school.meetings.repository.NotificationRepository;
import com.school.meetings.repository.OnlineMeetingRepository;
import com.school.parents.model.Parent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Google Meet links for online (VIDEO_CALL) check-ins and the emails that
 * deliver them to parents. Driven by {@link MeetingAutomationJob}, by the
 * "create link / send invite now" buttons, and by reschedule/cancel events.
 *
 * <p>Every public method records failures in the database instead of
 * throwing, so one bad follow-up never blocks the others and the reason is
 * visible on the dashboard.
 */
@Service
public class MeetingService {

    private static final Logger log = LoggerFactory.getLogger(MeetingService.class);

    public static final EnumSet<FollowUpStatus> OPEN =
            EnumSet.of(FollowUpStatus.SCHEDULED, FollowUpStatus.CONFIRMED, FollowUpStatus.RESCHEDULED);

    private final FollowUpRepository followUpRepository;
    private final OnlineMeetingRepository meetingRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final MeetingProvider provider;
    private final ParentMailer mailer;
    private final MeetingProperties properties;
    private final GoogleOAuthService oauth;
    private final GoogleAccountRepository googleAccounts;

    public MeetingService(FollowUpRepository followUpRepository, OnlineMeetingRepository meetingRepository,
                          NotificationRepository notificationRepository, UserRepository userRepository,
                          MeetingProvider provider, ParentMailer mailer, MeetingProperties properties,
                          GoogleOAuthService oauth, GoogleAccountRepository googleAccounts) {
        this.followUpRepository = followUpRepository;
        this.meetingRepository = meetingRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.provider = provider;
        this.mailer = mailer;
        this.properties = properties;
        this.oauth = oauth;
        this.googleAccounts = googleAccounts;
    }

    // --- what the job should work on ------------------------------------------------

    @Transactional(readOnly = true)
    public List<UUID> followUpsNeedingMeeting(LocalDateTime now) {
        if (!oauth.isConfigured() || googleAccounts.count() == 0) {
            return List.of();
        }
        return meetingRepository.findFollowUpsNeedingMeeting(OPEN, now, now.plus(properties.createLead()),
                properties.maxAttempts());
    }

    @Transactional(readOnly = true)
    public List<UUID> followUpsNeedingInvite(LocalDateTime now) {
        return meetingRepository.findFollowUpsNeedingInvite(OPEN, now, now.plus(properties.inviteLead()));
    }

    // --- meeting link ---------------------------------------------------------------------

    /** Creates the Google event + Meet link (or picks up a link Google was still provisioning). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OnlineMeeting ensureMeeting(UUID followUpId) {
        FollowUp followUp = load(followUpId);
        if (followUp.getType() != MeetingType.VIDEO_CALL) {
            throw new BadRequestException("Only VIDEO_CALL follow-ups get a Google Meet link");
        }
        if (!OPEN.contains(followUp.getStatus())) {
            throw new BadRequestException("This follow-up is " + followUp.getStatus());
        }
        OnlineMeeting meeting = meetingRepository.findByFollowUpId(followUpId)
                .orElseGet(() -> meetingRepository.save(new OnlineMeeting(followUp)));
        if (meeting.getStatus() == MeetingStatus.CREATED && meeting.getJoinUrl() != null) {
            return meeting;
        }
        if (!oauth.isConfigured()) {
            meeting.markFailed(oauth.notConfiguredReason());
            return meeting;
        }
        try {
            CreatedMeeting created;
            UUID accountId;
            String accountEmail;
            if (meeting.getProviderEventId() != null) {
                accountId = meeting.getGoogleAccountId();
                accountEmail = meeting.getOrganizerEmail();
                created = provider.fetch(accountId, meeting.getProviderEventId());
            } else {
                GoogleAccount host = hostFor(followUp);
                accountId = host.getId();
                accountEmail = host.getGoogleEmail();
                created = provider.create(accountId, request(followUp));
            }
            meeting.markCreated(created.eventId(), accountId, accountEmail, created.joinUrl());
            if (created.joinUrl() == null) {
                // Event exists but Meet is still provisioning — remember it and pick the link up next run.
                meeting.markFailed("Google is still creating the Meet link — will retry");
            } else {
                log.info("Meet link created for follow-up {} on {}", followUpId, accountEmail);
            }
        } catch (RuntimeException e) {
            log.warn("Could not create a Meet link for follow-up {}: {}", followUpId, e.getMessage());
            meeting.markFailed(e.getMessage());
        }
        return meeting;
    }

    private MeetingRequest request(FollowUp f) {
        String studentName = f.getStudent().getFirstName() + " " + f.getStudent().getLastName();
        return new MeetingRequest(
                "followup-" + f.getId(),
                "Check-in: " + studentName + " — " + properties.centerName(),
                "Online parent check-in for " + studentName + ".\nManaged by the Follow-Up app — reschedule or "
                        + "cancel it there, not in Google Calendar.",
                f.getScheduledStart(), f.getScheduledEnd(), properties.zone(),
                recipients(f).keySet().stream().toList());
    }

    /**
     * Who hosts: the assigned staff member's own Google account if they connected
     * one (and it still works); otherwise the shared center account.
     */
    GoogleAccount hostFor(FollowUp f) {
        if (f.getAssignedStaffId() != null) {
            var own = googleAccounts.findByUserId(f.getAssignedStaffId()).filter(a -> a.getLastError() == null);
            if (own.isPresent()) {
                return own.get();
            }
        }
        return googleAccounts.findFirstByCenterTrue().orElseThrow(() -> new IllegalStateException(
                "No Google account is connected — an admin can connect the center account in Settings"));
    }

    // --- parent emails --------------------------------------------------------------------------

    /**
     * Emails the Meet link to the parent(s). Skips recipients who already got
     * it for this time unless {@code force}. Returns how many emails went out.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int sendInvite(UUID followUpId, boolean force) {
        FollowUp followUp = load(followUpId);
        OnlineMeeting meeting = meetingRepository.findByFollowUpId(followUpId)
                .filter(m -> m.getStatus() == MeetingStatus.CREATED && m.getJoinUrl() != null)
                .orElseThrow(() -> new BadRequestException("This follow-up has no Meet link yet"));
        if (!OPEN.contains(followUp.getStatus())) {
            throw new BadRequestException("This follow-up is " + followUp.getStatus());
        }
        Map<String, Parent> recipients = recipients(followUp);
        if (recipients.isEmpty()) {
            throw new BadRequestException("No parent with an email address is linked to this student");
        }
        int sent = 0;
        for (Map.Entry<String, Parent> r : recipients.entrySet()) {
            if (deliver(NotificationKind.MEETING_INVITE, followUp, meeting.getJoinUrl(), r.getKey(), r.getValue(),
                    followUp.getScheduledStart(), force)) {
                sent++;
            }
        }
        return sent;
    }

    // --- keeping Google + parents in sync -------------------------------------------------------

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onRescheduled(UUID followUpId, LocalDateTime previousStart) {
        FollowUp followUp = load(followUpId);
        meetingRepository.findByFollowUpId(followUpId)
                .filter(m -> m.getStatus() == MeetingStatus.CREATED)
                .ifPresent(meeting -> {
                    try {
                        provider.reschedule(meeting.getGoogleAccountId(), meeting.getProviderEventId(),
                                followUp.getScheduledStart(), followUp.getScheduledEnd(), properties.zone());
                    } catch (RuntimeException e) {
                        // Link still works; only the calendar copy has the old time. Keep CREATED, show why.
                        log.warn("Could not move the Google event for follow-up {}: {}", followUpId, e.getMessage());
                        meeting.noteError("Google Calendar still shows the old time: " + e.getMessage());
                    }
                    // Only tell parents if they had already been sent the old time.
                    if (notificationRepository.findInviteSentAt(followUpId, previousStart) != null) {
                        recipients(followUp).forEach((email, parent) -> deliver(NotificationKind.MEETING_RESCHEDULED,
                                followUp, meeting.getJoinUrl(), email, parent, followUp.getScheduledStart(), false));
                    }
                });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCancelled(UUID followUpId) {
        FollowUp followUp = load(followUpId);
        meetingRepository.findByFollowUpId(followUpId).ifPresent(meeting -> {
            if (meeting.getProviderEventId() != null) {
                try {
                    provider.delete(meeting.getGoogleAccountId(), meeting.getProviderEventId());
                } catch (RuntimeException e) {
                    log.warn("Could not delete the Google event for follow-up {}: {}", followUpId, e.getMessage());
                }
            }
            meeting.markCancelled();
            if (notificationRepository.findInviteSentAt(followUpId, followUp.getScheduledStart()) != null) {
                recipients(followUp).forEach((email, parent) -> deliver(NotificationKind.MEETING_CANCELLED,
                        followUp, null, email, parent, followUp.getScheduledStart(), false));
            }
        });
    }

    // --- helpers ------------------------------------------------------------------------------------

    /** The meeting's parent if they have an email; otherwise every linked parent with one. Key = email. */
    Map<String, Parent> recipients(FollowUp f) {
        Map<String, Parent> result = new LinkedHashMap<>();
        Parent primary = f.getParent();
        if (primary != null && hasEmail(primary)) {
            result.put(primary.getEmail().trim(), primary);
            return result;
        }
        List<Parent> parents = new ArrayList<>(f.getStudent().getParents());
        for (Parent p : parents) {
            if (hasEmail(p)) {
                result.putIfAbsent(p.getEmail().trim(), p);
            }
        }
        return result;
    }

    private boolean deliver(NotificationKind kind, FollowUp f, String joinUrl, String email, Parent parent,
                            LocalDateTime scheduledFor, boolean force) {
        Notification notification = notificationRepository
                .findByFollowUpIdAndKindAndRecipientAndScheduledFor(f.getId(), kind, email, scheduledFor)
                .orElseGet(() -> notificationRepository.save(new Notification(f, kind, email, scheduledFor)));
        if ("SENT".equals(notification.getStatus()) && !force) {
            return false;
        }
        try {
            mailer.send(kind, new ParentMailer.MeetingEmail(email, parent.getFirstName(),
                    f.getStudent().getFirstName(), staffName(f), f.getScheduledStart(),
                    (int) Duration.between(f.getScheduledStart(), f.getScheduledEnd()).toMinutes(), joinUrl));
            notification.markSent();
            log.info("{} emailed for follow-up {}", kind, f.getId());
            return true;
        } catch (RuntimeException e) {
            log.warn("{} email for follow-up {} failed: {}", kind, f.getId(), e.getMessage());
            notification.markFailed(e.getMessage());
            return false;
        }
    }

    private String staffName(FollowUp f) {
        if (f.getAssignedStaffId() == null) {
            return null;
        }
        return userRepository.findById(f.getAssignedStaffId())
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse(null);
    }

    private static boolean hasEmail(Parent p) {
        return p.getEmail() != null && !p.getEmail().isBlank();
    }

    private FollowUp load(UUID id) {
        return followUpRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No follow-up with id " + id));
    }
}
