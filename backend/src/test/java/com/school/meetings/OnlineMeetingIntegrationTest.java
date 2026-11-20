package com.school.meetings;

import com.school.auth.model.Role;
import com.school.auth.service.JwtService;
import com.school.meetings.provider.MeetingProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Online check-ins end to end against the real DB, with a fake Google
 * Calendar and a mock mail sender. NOT @Transactional: MeetingService runs
 * in its own transactions (REQUIRES_NEW / after-commit listeners), which
 * can't see a test's uncommitted rows — so this test commits and cleans up.
 */
@SpringBootTest(properties = {
        "app.meetings.google.client-id=test-client-id",
        "app.meetings.google.client-secret=test-secret",
        "app.meetings.google.redirect-uri=http://localhost:8080/api/google/oauth/callback"})
@AutoConfigureMockMvc
class OnlineMeetingIntegrationTest {

    static final String LINK = "https://meet.google.com/abc-defg-hij";

    /** Records calls instead of talking to Google. */
    static class FakeGoogle implements MeetingProvider {
        final List<String> calls = new ArrayList<>();

        @Override
        public CreatedMeeting create(UUID accountId, MeetingRequest request) {
            calls.add("create:" + accountId + ":" + request.attendeeEmails());
            return new CreatedMeeting("evt-" + request.requestId(), LINK);
        }

        @Override
        public CreatedMeeting fetch(UUID accountId, String eventId) {
            return new CreatedMeeting(eventId, LINK);
        }

        @Override
        public void reschedule(UUID accountId, String eventId, LocalDateTime start, LocalDateTime end, ZoneId zone) {
            calls.add("reschedule:" + start);
        }

        @Override
        public void delete(UUID accountId, String eventId) {
            calls.add("delete:" + eventId);
        }
    }

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        FakeGoogle fakeGoogle() {
            return new FakeGoogle();
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private FakeGoogle google;
    @Autowired
    private MeetingService meetingService;

    @MockitoBean
    private JavaMailSender mailSender;

    private final List<String> parentIds = new ArrayList<>();
    private final List<String> studentIds = new ArrayList<>();

    private String auth() {
        return "Bearer " + jwtService.generateToken("staff-test@example.com", Role.ADMIN);
    }

    private JsonNode postJson(String url, ObjectNode body, int expectedStatus) throws Exception {
        String response = mockMvc.perform(post(url)
                        .header("Authorization", auth())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return response.isEmpty() ? null : objectMapper.readTree(response);
    }

    private String createFamily(String parentEmail) throws Exception {
        ObjectNode parent = objectMapper.createObjectNode();
        parent.put("firstName", "Mona");
        parent.put("lastName", "Meetwell");
        parent.put("relationship", "Mother");
        parent.put("preferredContactMethod", "EMAIL");
        if (parentEmail != null) {
            parent.put("email", parentEmail);
        }
        String parentId = postJson("/api/parents", parent, 201).get("id").asText();
        parentIds.add(parentId);

        ObjectNode student = objectMapper.createObjectNode();
        student.put("firstName", "Yasmin");
        student.put("lastName", "Meetwell");
        student.put("enrollmentDate", "2026-01-01");
        student.put("status", "ACTIVE");
        student.putArray("parentIds").add(parentId);
        String studentId = postJson("/api/students", student, 201).get("id").asText();
        studentIds.add(studentId);
        return studentId;
    }

    private String createFollowUp(String studentId, String type, LocalDateTime start) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("studentId", studentId);
        body.put("type", type);
        body.put("scheduledStart", start.toString());
        body.put("scheduledEnd", start.plusMinutes(30).toString());
        return postJson("/api/follow-ups", body, 201).get("id").asText();
    }

    /** A center Google account for the test, unless the dev DB already has a real one. */
    private UUID testCenterAccountId;

    @BeforeEach
    void connectCenterAccount() {
        Integer existing = jdbc.queryForObject("SELECT count(*) FROM google_accounts WHERE center", Integer.class);
        if (existing == 0) {
            testCenterAccountId = UUID.randomUUID();
            jdbc.update("INSERT INTO google_accounts (id, center, google_email, refresh_token_enc) VALUES (?, true, ?, ?)",
                    testCenterAccountId, "center-test@gmail.test", "not-a-real-token");
        }
    }

    @AfterEach
    void cleanUp() {
        for (String studentId : studentIds) {
            UUID id = UUID.fromString(studentId);
            jdbc.update("DELETE FROM notifications WHERE follow_up_id IN (SELECT id FROM follow_ups WHERE student_id = ?)", id);
            jdbc.update("DELETE FROM online_meetings WHERE follow_up_id IN (SELECT id FROM follow_ups WHERE student_id = ?)", id);
            jdbc.update("DELETE FROM follow_ups WHERE student_id = ?", id);
            jdbc.update("DELETE FROM students WHERE id = ?", id);
        }
        for (String parentId : parentIds) {
            jdbc.update("DELETE FROM parents WHERE id = ?", UUID.fromString(parentId));
        }
        studentIds.clear();
        parentIds.clear();
        google.calls.clear();
        if (testCenterAccountId != null) {
            jdbc.update("DELETE FROM google_accounts WHERE id = ?", testCenterAccountId);
            testCenterAccountId = null;
        }
    }

    @Test
    void connectingGoogleRequiresAdminForTheCenterAccountAndRejectsForgedCallbacks() throws Exception {
        String staff = "Bearer " + jwtService.generateToken("staff@example.com", Role.STAFF);
        mockMvc.perform(post("/api/google/accounts/connect").param("kind", "CENTER").header("Authorization", staff))
                .andExpect(status().isForbidden());

        String url = objectMapper.readTree(mockMvc.perform(post("/api/google/accounts/connect")
                        .param("kind", "PERSONAL").header("Authorization", staff))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("url").asText();
        assertThat(url).startsWith("https://accounts.google.com/").contains("client_id=test-client-id")
                .contains("access_type=offline").contains("calendar.events").contains("state=");

        // The callback is public, but a forged/expired state never reaches Google and sends the user back with an error.
        mockMvc.perform(get("/api/google/oauth/callback").param("code", "x").param("state", "forged"))
                .andExpect(status().isFound())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Location",
                        org.hamcrest.Matchers.containsString("/settings?google=error")));
        // A login token is not a valid state.
        mockMvc.perform(get("/api/google/oauth/callback").param("code", "x")
                        .param("state", jwtService.generateToken("admin@example.com", Role.ADMIN)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Location",
                        org.hamcrest.Matchers.containsString("google=error")));
    }

    @Test
    void videoCallGetsAMeetLinkAndTheParentGetsOneInviteThenRescheduleAndCancelAreSynced() throws Exception {
        String studentId = createFamily("mona.meetwell@example.com");
        LocalDateTime start = LocalDateTime.now().plusHours(3).withSecond(0).withNano(0);
        String followUpId = createFollowUp(studentId, "VIDEO_CALL", start);
        UUID id = UUID.fromString(followUpId);

        // The job would pick it up (within 48h, no link yet).
        assertThat(meetingService.followUpsNeedingMeeting(LocalDateTime.now())).contains(id);

        postJson("/api/follow-ups/" + followUpId + "/meeting", objectMapper.createObjectNode(), 200);
        assertThat(google.calls).anyMatch(c -> c.startsWith("create:") && c.contains("mona.meetwell@example.com"));
        assertThat(meetingService.followUpsNeedingMeeting(LocalDateTime.now())).doesNotContain(id);

        // Within 24h with a link and no invite yet → due for the invite.
        assertThat(meetingService.followUpsNeedingInvite(LocalDateTime.now())).contains(id);
        JsonNode sent = postJson("/api/follow-ups/" + followUpId + "/meeting/invite", objectMapper.createObjectNode(), 200);
        assertThat(sent.get("sent").asInt()).isEqualTo(1);
        verify(mailSender).send(any(SimpleMailMessage.class));
        assertThat(meetingService.followUpsNeedingInvite(LocalDateTime.now())).doesNotContain(id);

        // Idempotent: a second run sends nothing.
        clearInvocations(mailSender);
        assertThat(meetingService.sendInvite(id, false)).isZero();
        verify(mailSender, never()).send(any(SimpleMailMessage.class));

        // Dashboard shows the link and that the invite went out.
        mockMvc.perform(get("/api/follow-ups/agenda")
                        .header("Authorization", auth())
                        .param("date", start.toLocalDate().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + followUpId + "')].meeting.status").value("CREATED"))
                .andExpect(jsonPath("$[?(@.id == '" + followUpId + "')].meeting.joinUrl").value(LINK))
                .andExpect(jsonPath("$[?(@.id == '" + followUpId + "')].meeting.inviteSentAt").isNotEmpty());

        // Reschedule → Google event moved, parent told the new time.
        ObjectNode moved = objectMapper.createObjectNode();
        moved.put("scheduledStart", start.plusHours(1).toString());
        moved.put("scheduledEnd", start.plusHours(1).plusMinutes(30).toString());
        postJson("/api/follow-ups/" + followUpId + "/reschedule", moved, 200);
        assertThat(google.calls).anyMatch(c -> c.startsWith("reschedule:"));
        verify(mailSender, atLeastOnce()).send(any(SimpleMailMessage.class));
        // The reschedule email counts as the invite for the new time.
        assertThat(meetingService.followUpsNeedingInvite(LocalDateTime.now())).doesNotContain(id);

        // Cancel → Google event deleted.
        postJson("/api/follow-ups/" + followUpId + "/cancel", objectMapper.createObjectNode(), 200);
        assertThat(google.calls).anyMatch(c -> c.startsWith("delete:"));
        Integer cancelledMails = jdbc.queryForObject(
                "SELECT count(*) FROM notifications WHERE follow_up_id = ? AND kind = 'MEETING_CANCELLED' AND status = 'SENT'",
                Integer.class, id);
        assertThat(cancelledMails).isEqualTo(1);
    }

    @Test
    void nonVideoFollowUpsGetNoMeetingAndMissingParentEmailIsReported() throws Exception {
        String studentId = createFamily(null);
        LocalDateTime start = LocalDateTime.now().plusHours(5).withSecond(0).withNano(0);

        String phoneCall = createFollowUp(studentId, "PHONE_CALL", start);
        postJson("/api/follow-ups/" + phoneCall + "/meeting", objectMapper.createObjectNode(), 400);
        assertThat(meetingService.followUpsNeedingMeeting(LocalDateTime.now())).doesNotContain(UUID.fromString(phoneCall));

        String video = createFollowUp(studentId, "VIDEO_CALL", start.plusHours(1));
        postJson("/api/follow-ups/" + video + "/meeting", objectMapper.createObjectNode(), 200);
        // Link exists, but no parent email → the invite is refused with a clear message.
        mockMvc.perform(post("/api/follow-ups/" + video + "/meeting/invite").header("Authorization", auth()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("email")));
        mockMvc.perform(get("/api/follow-ups/agenda")
                        .header("Authorization", auth())
                        .param("date", start.toLocalDate().toString()))
                .andExpect(jsonPath("$[?(@.id == '" + video + "')].meeting.parentEmailMissing").value(true));
    }
}
