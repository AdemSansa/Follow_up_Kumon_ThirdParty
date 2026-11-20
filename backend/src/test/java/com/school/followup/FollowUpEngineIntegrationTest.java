package com.school.followup;

import com.school.auth.model.Role;
import com.school.auth.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack test (real Postgres) for Sprint 4: generating occurrences
 * from a plan, overdue detection, rescheduling, cancellation, and the
 * "paused student / inactive plan generates nothing" rule.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FollowUpEngineIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    private String authHeader() {
        return "Bearer " + jwtService.generateToken("staff-test@example.com", Role.ADMIN);
    }

    private String createStudent(String firstName, String lastName) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("firstName", firstName);
        body.put("lastName", lastName);
        body.put("enrollmentDate", "2026-01-01");
        body.put("status", "ACTIVE");
        body.putArray("subjects").add("MATH");

        String response = mockMvc.perform(post("/api/students")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private String createPlan(String studentId, String frequency, String startDate) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("studentId", studentId);
        body.put("frequencyType", frequency);
        body.put("startDate", startDate);
        body.put("meetingType", "PARENT_MEETING");

        String response = mockMvc.perform(post("/api/follow-up-plans")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private ObjectNode countedPlanBody(String studentId, int occurrenceCount) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("studentId", studentId);
        body.put("frequencyType", "WEEKLY");
        body.put("startDate", LocalDate.now().plusDays(1).toString());
        body.put("meetingType", "PARENT_MEETING");
        body.put("occurrenceCount", occurrenceCount);
        return body;
    }

    private long occurrencesOf(String planId) throws Exception {
        String response = mockMvc.perform(get("/api/follow-ups")
                        .header("Authorization", authHeader())
                        .param("followUpPlanId", planId)
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("totalElements").asLong();
    }

    @Test
    void agendaListsTheDaysCheckInsWithContactAndPlanDetails() throws Exception {
        ObjectNode parent = objectMapper.createObjectNode();
        parent.put("firstName", "Leila");
        parent.put("lastName", "Agenda");
        parent.put("phone", "+216 20 111 222");
        parent.put("email", "leila.agenda@example.com");
        parent.put("relationship", "Mother");
        parent.put("preferredContactMethod", "WHATSAPP");
        String parentId = objectMapper.readTree(mockMvc.perform(post("/api/parents")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(parent)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        ObjectNode student = objectMapper.createObjectNode();
        student.put("firstName", "Rami");
        student.put("lastName", "Agenda");
        student.put("enrollmentDate", "2026-01-01");
        student.put("status", "ACTIVE");
        student.put("level", "C");
        student.putArray("subjects").add("MATH");
        student.putArray("parentIds").add(parentId);
        String studentId = objectMapper.readTree(mockMvc.perform(post("/api/students")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(student)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        ObjectNode plan = countedPlanBody(studentId, 5);
        plan.put("startDate", LocalDate.now().toString());
        plan.put("preferredTime", "23:30:00");
        plan.put("parentId", parentId);
        plan.put("location", "Room 2");
        mockMvc.perform(post("/api/follow-up-plans")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(plan)))
                .andExpect(status().isCreated());

        String agenda = mockMvc.perform(get("/api/follow-ups/agenda")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode item = null;
        for (JsonNode candidate : objectMapper.readTree(agenda)) {
            if (candidate.get("student").get("id").asText().equals(studentId)) {
                item = candidate;
            }
        }
        org.assertj.core.api.Assertions.assertThat(item).as("today's check-in for the new student").isNotNull();
        org.assertj.core.api.Assertions.assertThat(item.get("location").asText()).isEqualTo("Room 2");
        org.assertj.core.api.Assertions.assertThat(item.get("student").get("level").asText()).isEqualTo("C");
        JsonNode contact = item.get("contacts").get(0);
        org.assertj.core.api.Assertions.assertThat(contact.get("primary").asBoolean()).isTrue();
        org.assertj.core.api.Assertions.assertThat(contact.get("phone").asText()).isEqualTo("+216 20 111 222");
        org.assertj.core.api.Assertions.assertThat(contact.get("preferredContactMethod").asText()).isEqualTo("WHATSAPP");
        org.assertj.core.api.Assertions.assertThat(item.get("plan").get("occurrenceNumber").asLong()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(item.get("plan").get("occurrenceCount").asInt()).isEqualTo(5);
        org.assertj.core.api.Assertions.assertThat(item.get("lastCheckIn").isNull()).isTrue();

        // Another day doesn't include it (weekly plan → next one is 7 days later).
        String tomorrow = mockMvc.perform(get("/api/follow-ups/agenda")
                        .header("Authorization", authHeader())
                        .param("date", LocalDate.now().plusDays(1).toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(tomorrow).doesNotContain(studentId);
    }

    @Test
    void planWithOccurrenceCountGeneratesExactlyThatManyEvenPastTheRollingWindow() throws Exception {
        String studentId = createStudent("Hana", "Rekik");
        // 40 weekly check-ins span ~9 months, past the 6-month rolling window:
        // a fixed-count plan is generated in full up front.
        String response = mockMvc.perform(post("/api/follow-up-plans")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(countedPlanBody(studentId, 40))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.occurrenceCount").value(40))
                .andReturn().getResponse().getContentAsString();
        String planId = objectMapper.readTree(response).get("id").asText();

        org.assertj.core.api.Assertions.assertThat(occurrencesOf(planId)).isEqualTo(40);

        // Lowering the count on edit trims the not-yet-happened occurrences.
        mockMvc.perform(put("/api/follow-up-plans/" + planId)
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(countedPlanBody(studentId, 10))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.occurrenceCount").value(10));
        org.assertj.core.api.Assertions.assertThat(occurrencesOf(planId)).isEqualTo(10);

        // Clearing it makes the plan ongoing again (rolling window, not 10).
        ObjectNode ongoing = countedPlanBody(studentId, 1);
        ongoing.putNull("occurrenceCount");
        mockMvc.perform(put("/api/follow-up-plans/" + planId)
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(ongoing)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.occurrenceCount").doesNotExist());
        org.assertj.core.api.Assertions.assertThat(occurrencesOf(planId)).isBetween(20L, 30L);
    }

    @Test
    void occurrenceCountOutOfRangeIsRejected() throws Exception {
        String studentId = createStudent("Zied", "Amri");
        for (int bad : new int[] {0, 201}) {
            mockMvc.perform(post("/api/follow-up-plans")
                            .header("Authorization", authHeader())
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(countedPlanBody(studentId, bad))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void creatingAMonthlyPlanGeneratesOccurrencesWithinTheRollingWindow() throws Exception {
        String studentId = createStudent("Nadia", "Cherif");
        String planId = createPlan(studentId, "MONTHLY", "2026-09-01");

        String listResponse = mockMvc.perform(get("/api/follow-ups")
                        .header("Authorization", authHeader())
                        .param("followUpPlanId", planId)
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode content = objectMapper.readTree(listResponse).get("content");

        // 6-month rolling window, monthly cadence starting today-ish -> at
        // least a handful of occurrences, and every one SCHEDULED with the
        // plan's meeting type.
        org.assertj.core.api.Assertions.assertThat(content.size()).isGreaterThanOrEqualTo(1);
        for (JsonNode occurrence : content) {
            org.assertj.core.api.Assertions.assertThat(occurrence.get("status").asText()).isEqualTo("SCHEDULED");
            org.assertj.core.api.Assertions.assertThat(occurrence.get("type").asText()).isEqualTo("PARENT_MEETING");
        }

        mockMvc.perform(get("/api/follow-up-plans/" + planId)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextFollowUpDate").exists());
    }

    @Test
    void pausedPlanGeneratesNoOccurrences() throws Exception {
        String studentId = createStudent("Omar", "Fassi");

        ObjectNode body = objectMapper.createObjectNode();
        body.put("studentId", studentId);
        body.put("frequencyType", "WEEKLY");
        body.put("startDate", "2026-09-01");
        body.put("meetingType", "PHONE_CALL");
        body.put("active", false);

        String response = mockMvc.perform(post("/api/follow-up-plans")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String planId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/api/follow-ups")
                        .header("Authorization", authHeader())
                        .param("followUpPlanId", planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void pausedStudentGeneratesNoOccurrences() throws Exception {
        String studentId = createStudent("Lina", "Idrissi");

        // Pause the student itself (not the plan) after creating it.
        ObjectNode updateBody = objectMapper.createObjectNode();
        updateBody.put("firstName", "Lina");
        updateBody.put("lastName", "Idrissi");
        updateBody.put("enrollmentDate", "2026-01-01");
        updateBody.put("status", "PAUSED");
        updateBody.putArray("subjects").add("MATH");
        mockMvc.perform(put("/api/students/" + studentId)
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk());

        String planId = createPlan(studentId, "WEEKLY", "2026-09-01");

        mockMvc.perform(get("/api/follow-ups")
                        .header("Authorization", authHeader())
                        .param("followUpPlanId", planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void updatingAPlanRegeneratesFutureOccurrencesWithoutDuplicating() throws Exception {
        String studentId = createStudent("Tarek", "Belhadj");
        // A startDate safely after "now" (so every generated occurrence is
        // a not-yet-happened SCHEDULED one that regenerateFuture() is
        // allowed to touch) but still well within the 6-month generation
        // window — isolates this test from the "past occurrences are
        // preserved" behavior covered elsewhere by an earlier startDate.
        String futureStartDate = LocalDate.now().plusDays(14).toString();
        String planId = createPlan(studentId, "MONTHLY", futureStartDate);

        String firstListResponse = mockMvc.perform(get("/api/follow-ups")
                        .header("Authorization", authHeader())
                        .param("followUpPlanId", planId)
                        .param("size", "20"))
                .andReturn().getResponse().getContentAsString();
        int firstCount = objectMapper.readTree(firstListResponse).get("totalElements").asInt();
        org.assertj.core.api.Assertions.assertThat(firstCount).isGreaterThan(0);

        ObjectNode updateBody = objectMapper.createObjectNode();
        updateBody.put("studentId", studentId);
        updateBody.put("frequencyType", "MONTHLY");
        updateBody.put("startDate", futureStartDate);
        updateBody.put("meetingType", "VIDEO_CALL");

        mockMvc.perform(put("/api/follow-up-plans/" + planId)
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk());

        String secondListResponse = mockMvc.perform(get("/api/follow-ups")
                        .header("Authorization", authHeader())
                        .param("followUpPlanId", planId)
                        .param("size", "20"))
                .andReturn().getResponse().getContentAsString();
        JsonNode secondContent = objectMapper.readTree(secondListResponse).get("content");

        // Same count (not doubled), and reflects the updated meeting type.
        org.assertj.core.api.Assertions.assertThat(secondContent.size()).isEqualTo(firstCount);
        for (JsonNode occurrence : secondContent) {
            org.assertj.core.api.Assertions.assertThat(occurrence.get("type").asText()).isEqualTo("VIDEO_CALL");
        }
    }

    @Test
    void listCanBeFilteredToACalendarDateRange() throws Exception {
        String studentId = createStudent("Karim", "Zaidi");
        String planId = createPlan(studentId, "MONTHLY", "2026-09-01");

        // Only the Sept occurrence falls in this window; Oct's shouldn't.
        String response = mockMvc.perform(get("/api/follow-ups")
                        .header("Authorization", authHeader())
                        .param("followUpPlanId", planId)
                        .param("from", "2026-09-01")
                        .param("to", "2026-09-30")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode content = objectMapper.readTree(response).get("content");

        org.assertj.core.api.Assertions.assertThat(content.size()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(content.get(0).get("scheduledStart").asText())
                .startsWith("2026-09-01");
    }

    @Test
    void reschedulingMovesTheOccurrenceWithoutTouchingThePlan() throws Exception {
        String studentId = createStudent("Sami", "Ouazzani");
        String planId = createPlan(studentId, "MONTHLY", "2026-09-01");

        String listResponse = mockMvc.perform(get("/api/follow-ups")
                        .header("Authorization", authHeader())
                        .param("followUpPlanId", planId)
                        .param("size", "1"))
                .andReturn().getResponse().getContentAsString();
        String followUpId = objectMapper.readTree(listResponse).get("content").get(0).get("id").asText();

        ObjectNode rescheduleBody = objectMapper.createObjectNode();
        rescheduleBody.put("scheduledStart", "2026-09-15T14:00:00");
        rescheduleBody.put("scheduledEnd", "2026-09-15T14:30:00");

        mockMvc.perform(post("/api/follow-ups/" + followUpId + "/reschedule")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(rescheduleBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESCHEDULED"))
                .andExpect(jsonPath("$.scheduledStart").value("2026-09-15T14:00:00"));

        // The plan's own rule is untouched.
        mockMvc.perform(get("/api/follow-up-plans/" + planId)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDate").value("2026-09-01"));
    }

    @Test
    void cancellingAFollowUpMarksItCancelledAndItIsExcludedFromOverdue() throws Exception {
        String studentId = createStudent("Rim", "Bensalem");

        ObjectNode body = objectMapper.createObjectNode();
        body.put("studentId", studentId);
        body.put("type", "PARENT_MEETING");
        body.put("scheduledStart", "2020-01-01T10:00:00");
        body.put("scheduledEnd", "2020-01-01T10:30:00");

        String response = mockMvc.perform(post("/api/follow-ups")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.overdue").value(true))
                .andReturn().getResponse().getContentAsString();
        String followUpId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(post("/api/follow-ups/" + followUpId + "/cancel")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.overdue").value(false));
    }

    @Test
    void deletingAPlanWithExistingFollowUpsIsRejected() throws Exception {
        String studentId = createStudent("Youssef", "Mansouri");
        String planId = createPlan(studentId, "MONTHLY", "2026-09-01");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/follow-up-plans/" + planId)
                        .header("Authorization", authHeader()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/follow-ups"))
                .andExpect(status().isUnauthorized());
    }
}
