package com.school.followup;

import com.school.auth.model.Role;
import com.school.auth.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Full-stack (real Postgres) tests for Sprint 6's check-in completion flow. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CheckInIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    private String authHeader() {
        return "Bearer " + jwtService.generateToken("staff-test@example.com", Role.ADMIN);
    }

    /** Creates a student plus one ad-hoc follow-up for them; returns the follow-up id. */
    private String createFollowUp() throws Exception {
        ObjectNode student = objectMapper.createObjectNode();
        student.put("firstName", "Dina");
        student.put("lastName", "Rahmani");
        student.put("enrollmentDate", "2026-01-01");
        student.put("status", "ACTIVE");
        String studentResponse = mockMvc.perform(post("/api/students")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(student)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String studentId = objectMapper.readTree(studentResponse).get("id").asText();

        ObjectNode followUp = objectMapper.createObjectNode();
        followUp.put("studentId", studentId);
        followUp.put("type", "PARENT_MEETING");
        followUp.put("scheduledStart", "2026-09-20T10:00:00");
        followUp.put("scheduledEnd", "2026-09-20T10:30:00");
        String response = mockMvc.perform(post("/api/follow-ups")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(followUp)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private ObjectNode validCheckIn() {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("progressRating", 4);
        body.put("engagementRating", 5);
        body.put("parentSatisfactionRating", 3);
        body.putArray("concerns").add("HOMEWORK").add("MOTIVATION");
        body.put("summary", "Good progress this month.");
        body.put("recommendations", "Continue regular practice.");
        return body;
    }

    @Test
    void completingAFollowUpRecordsTheCheckInAndMarksItCompleted() throws Exception {
        String followUpId = createFollowUp();

        mockMvc.perform(post("/api/follow-ups/" + followUpId + "/complete")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validCheckIn())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.progressRating").value(4))
                .andExpect(jsonPath("$.engagementRating").value(5))
                .andExpect(jsonPath("$.parentSatisfactionRating").value(3))
                .andExpect(jsonPath("$.concerns.length()").value(2))
                .andExpect(jsonPath("$.summary").value("Good progress this month."))
                .andExpect(jsonPath("$.completedAt").exists())
                .andExpect(jsonPath("$.overdue").value(false));

        mockMvc.perform(get("/api/follow-ups/" + followUpId).header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void completionCreatesTheRequestedActionItems() throws Exception {
        String followUpId = createFollowUp();

        ObjectNode body = validCheckIn();
        body.putArray("actionItems")
                .addObject().put("title", "Monitor homework").put("dueDate", "2026-10-01").put("priority", "HIGH");

        mockMvc.perform(post("/api/follow-ups/" + followUpId + "/complete")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/action-items")
                        .header("Authorization", authHeader())
                        .param("followUpId", followUpId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Monitor homework"))
                .andExpect(jsonPath("$.content[0].priority").value("HIGH"))
                .andExpect(jsonPath("$.content[0].status").value("TODO"));
    }

    @Test
    void actionItemCanBeMarkedDone() throws Exception {
        String followUpId = createFollowUp();
        ObjectNode body = validCheckIn();
        body.putArray("actionItems").addObject().put("title", "Call parent");
        mockMvc.perform(post("/api/follow-ups/" + followUpId + "/complete")
                .header("Authorization", authHeader())
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(body)));

        String list = mockMvc.perform(get("/api/action-items")
                        .header("Authorization", authHeader())
                        .param("followUpId", followUpId))
                .andReturn().getResponse().getContentAsString();
        String actionId = objectMapper.readTree(list).get("content").get(0).get("id").asText();

        mockMvc.perform(post("/api/action-items/" + actionId + "/complete").header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.completedAt").exists());
    }

    @Test
    void ratingsOutsideOneToFiveAreRejected() throws Exception {
        String followUpId = createFollowUp();
        ObjectNode body = validCheckIn();
        body.put("progressRating", 6);

        mockMvc.perform(post("/api/follow-ups/" + followUpId + "/complete")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("progressRating"));
    }

    @Test
    void completingTwiceIsRejected() throws Exception {
        String followUpId = createFollowUp();
        String payload = objectMapper.writeValueAsString(validCheckIn());

        mockMvc.perform(post("/api/follow-ups/" + followUpId + "/complete")
                .header("Authorization", authHeader()).contentType("application/json").content(payload))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/follow-ups/" + followUpId + "/complete")
                        .header("Authorization", authHeader()).contentType("application/json").content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelledFollowUpCannotBeCompleted() throws Exception {
        String followUpId = createFollowUp();
        mockMvc.perform(post("/api/follow-ups/" + followUpId + "/cancel").header("Authorization", authHeader()));

        mockMvc.perform(post("/api/follow-ups/" + followUpId + "/complete")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validCheckIn())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownActionItemAssigneeIsRejected() throws Exception {
        String followUpId = createFollowUp();
        ObjectNode body = validCheckIn();
        body.putArray("actionItems").addObject().put("title", "Task").put("assignedTo", UUID.randomUUID().toString());

        mockMvc.perform(post("/api/follow-ups/" + followUpId + "/complete")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unauthenticatedCompletionIsRejected() throws Exception {
        mockMvc.perform(post("/api/follow-ups/" + UUID.randomUUID() + "/complete")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
