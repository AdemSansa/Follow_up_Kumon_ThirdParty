package com.school.meetings;

import com.school.meetings.google.GoogleOAuthService;
import com.school.meetings.model.OnlineMeeting;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/** Manual controls for online check-ins: "create link now", "send invite now", and setup status. */
@RestController
@RequestMapping("/api")
public class MeetingController {

    private final MeetingService meetingService;
    private final GoogleOAuthService oauth;
    private final MeetingProperties properties;
    private final String mailHost;

    public MeetingController(MeetingService meetingService, GoogleOAuthService oauth, MeetingProperties properties,
                             @Value("${spring.mail.host:}") String mailHost) {
        this.meetingService = meetingService;
        this.oauth = oauth;
        this.properties = properties;
        this.mailHost = mailHost;
    }

    public record MeetingResponse(UUID followUpId, String status, String joinUrl, String organizerEmail,
                                  String lastError) {
        static MeetingResponse from(OnlineMeeting m) {
            return new MeetingResponse(m.getFollowUp().getId(), m.getStatus().name(), m.getJoinUrl(),
                    m.getOrganizerEmail(), m.getLastError());
        }
    }

    @PostMapping("/follow-ups/{id}/meeting")
    public MeetingResponse createNow(@PathVariable UUID id) {
        return MeetingResponse.from(meetingService.ensureMeeting(id));
    }

    @PostMapping("/follow-ups/{id}/meeting/invite")
    public Map<String, Integer> sendInviteNow(@PathVariable UUID id,
                                              @RequestParam(defaultValue = "false") boolean resend) {
        return Map.of("sent", meetingService.sendInvite(id, resend));
    }

    @GetMapping("/meetings/status")
    public Map<String, Object> status() {
        boolean centerConnected = oauth.center().isPresent();
        String googleMessage = !oauth.isConfigured() ? oauth.notConfiguredReason()
                : !centerConnected ? "No center Google account is connected — an admin can connect one in Settings."
                : "";
        return Map.of(
                // "configured" = links can be created for anyone (server set up + center account connected).
                "googleConfigured", oauth.isConfigured() && centerConnected,
                "googleMessage", googleMessage,
                "emailConfigured", !mailHost.isBlank(),
                "automationEnabled", properties.automationEnabled(),
                "timezone", properties.zone().getId(),
                "createLeadHours", properties.createLead().toHours(),
                "inviteLeadHours", properties.inviteLead().toHours());
    }
}
