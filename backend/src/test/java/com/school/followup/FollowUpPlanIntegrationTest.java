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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack test (real Postgres, real Spring Security filter chain) for
 * Sprint 3's Follow-Up Plan CRUD. Rolled back after each test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FollowUpPlanIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    private String authHeader() {
        return "Bearer " + jwtService.generateToken("staff-test@example.com", Role.ADMIN);
    }

    private String createParent(String firstName, String lastName) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("firstName", firstName);
        body.put("lastName", lastName);
        body.put("relationship", "Mother");
        body.put("preferredContactMethod", "EMAIL");

        String response = mockMvc.perform(post("/api/parents")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private String createStudent(String firstName, String lastName, String parentId) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("firstName", firstName);
        body.put("lastName", lastName);
        body.put("enrollmentDate", "2026-09-01");
        body.put("status", "ACTIVE");
        body.putArray("subjects").add("MATH");
        if (parentId != null) {
            body.putArray("parentIds").add(parentId);
        }

        String response = mockMvc.perform(post("/api/students")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/follow-up-plans"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createsListsUpdatesAndDeletesAMonthlyPlan() throws Exception {
        String parentId = createParent("Salma", "Kadiri");
        String studentId = createStudent("Yanis", "Kadiri", parentId);

        ObjectNode createBody = objectMapper.createObjectNode();
        createBody.put("studentId", studentId);
        createBody.put("parentId", parentId);
        createBody.put("frequencyType", "MONTHLY");
        createBody.put("startDate", "2026-10-01");
        createBody.put("preferredDay", "SATURDAY");
        createBody.put("preferredTime", "10:00:00");
        createBody.put("meetingDurationMinutes", 30);
        createBody.put("meetingType", "PARENT_MEETING");
        createBody.putArray("reminderOffsets").add("TWENTY_FOUR_HOURS_BEFORE");

        String createResponse = mockMvc.perform(post("/api/follow-up-plans")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.student.firstName").value("Yanis"))
                .andExpect(jsonPath("$.parent.firstName").value("Salma"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.nextFollowUpDate").value("2026-10-01"))
                .andReturn().getResponse().getContentAsString();
        String planId = objectMapper.readTree(createResponse).get("id").asText();

        mockMvc.perform(get("/api/follow-up-plans")
                        .header("Authorization", authHeader())
                        .param("studentId", studentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(planId));

        ObjectNode updateBody = createBody.deepCopy();
        updateBody.put("active", false);
        mockMvc.perform(put("/api/follow-up-plans/" + planId)
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/follow-up-plans")
                        .header("Authorization", authHeader())
                        .param("studentId", studentId)
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(delete("/api/follow-up-plans/" + planId)
                        .header("Authorization", authHeader()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/follow-up-plans/" + planId)
                        .header("Authorization", authHeader()))
                .andExpect(status().isNotFound());
    }

    @Test
    void createsACustomFrequencyPlan() throws Exception {
        String studentId = createStudent("Ines", "Barhoumi", null);

        ObjectNode createBody = objectMapper.createObjectNode();
        createBody.put("studentId", studentId);
        createBody.put("frequencyType", "CUSTOM");
        createBody.put("customIntervalValue", 10);
        createBody.put("customIntervalUnit", "DAYS");
        createBody.put("startDate", "2026-10-01");
        createBody.put("meetingType", "PHONE_CALL");

        mockMvc.perform(post("/api/follow-up-plans")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customIntervalValue").value(10))
                .andExpect(jsonPath("$.customIntervalUnit").value("DAYS"));
    }

    @Test
    void customFrequencyWithoutIntervalIsRejected() throws Exception {
        String studentId = createStudent("Malak", "Cherif", null);

        ObjectNode createBody = objectMapper.createObjectNode();
        createBody.put("studentId", studentId);
        createBody.put("frequencyType", "CUSTOM");
        createBody.put("startDate", "2026-10-01");
        createBody.put("meetingType", "PHONE_CALL");

        mockMvc.perform(post("/api/follow-up-plans")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void parentNotLinkedToStudentIsRejected() throws Exception {
        String unrelatedParentId = createParent("Hind", "Alaoui");
        String studentId = createStudent("Rania", "Belkacem", null);

        ObjectNode createBody = objectMapper.createObjectNode();
        createBody.put("studentId", studentId);
        createBody.put("parentId", unrelatedParentId);
        createBody.put("frequencyType", "WEEKLY");
        createBody.put("startDate", "2026-10-01");
        createBody.put("meetingType", "INTERNAL_REVIEW");

        mockMvc.perform(post("/api/follow-up-plans")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownStudentIsRejected() throws Exception {
        ObjectNode createBody = objectMapper.createObjectNode();
        createBody.put("studentId", UUID.randomUUID().toString());
        createBody.put("frequencyType", "WEEKLY");
        createBody.put("startDate", "2026-10-01");
        createBody.put("meetingType", "INTERNAL_REVIEW");

        mockMvc.perform(post("/api/follow-up-plans")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownAssignedStaffIsRejected() throws Exception {
        String studentId = createStudent("Adam", "Naciri", null);

        ObjectNode createBody = objectMapper.createObjectNode();
        createBody.put("studentId", studentId);
        createBody.put("assignedStaffId", UUID.randomUUID().toString());
        createBody.put("frequencyType", "WEEKLY");
        createBody.put("startDate", "2026-10-01");
        createBody.put("meetingType", "INTERNAL_REVIEW");

        mockMvc.perform(post("/api/follow-up-plans")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isBadRequest());
    }
}
