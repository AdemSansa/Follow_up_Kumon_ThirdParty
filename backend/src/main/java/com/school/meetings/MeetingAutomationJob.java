package com.school.meetings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Background job (first piece of Sprint 7 automation): every poll interval,
 * creates Meet links for online check-ins coming up within createLead, then
 * emails parents whose check-in is within inviteLead. Off unless
 * MEETINGS_AUTOMATION_ENABLED=true.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.meetings.automation-enabled", havingValue = "true")
public class MeetingAutomationJob {

    private static final Logger log = LoggerFactory.getLogger(MeetingAutomationJob.class);

    private final MeetingService meetingService;

    public MeetingAutomationJob(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @Scheduled(initialDelayString = "PT20S", fixedDelayString = "${app.meetings.poll-interval}")
    public void run() {
        LocalDateTime now = LocalDateTime.now();
        int links = 0;
        for (UUID id : meetingService.followUpsNeedingMeeting(now)) {
            try {
                meetingService.ensureMeeting(id);
                links++;
            } catch (RuntimeException e) {
                log.warn("Meet link for follow-up {} skipped: {}", id, e.getMessage());
            }
        }
        int emails = 0;
        for (UUID id : meetingService.followUpsNeedingInvite(now)) {
            try {
                emails += meetingService.sendInvite(id, false);
            } catch (RuntimeException e) {
                log.warn("Invite for follow-up {} skipped: {}", id, e.getMessage());
            }
        }
        if (links > 0 || emails > 0) {
            log.info("Meeting automation: {} link attempt(s), {} invite email(s) sent", links, emails);
        }
    }
}
