package com.school.students;

import com.school.auth.model.Role;
import com.school.auth.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
 * the Sprint 2 CRUD + linking + filtering flow. Rolled back after each test
 * so repeated runs don't accumulate data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StudentParentIntegrationTest {

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
        body.put("phone", "555-0100");
        body.put("email", firstName.toLowerCase() + "." + lastName.toLowerCase() + "@example.com");
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

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createsListsUpdatesAndDeletesAStudentLinkedToAParent() throws Exception {
        String parentId = createParent("Amina", "BenAli");

        ObjectNode createBody = objectMapper.createObjectNode();
        createBody.put("firstName", "Ahmed");
        createBody.put("lastName", "BenAli");
        createBody.put("enrollmentDate", "2026-09-01");
        createBody.put("status", "ACTIVE");
        createBody.put("level", "A");
        createBody.putArray("subjects").add("MATH").add("ENGLISH");
        createBody.putArray("parentIds").add(parentId);

        String createResponse = mockMvc.perform(post("/api/students")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Ahmed"))
                .andExpect(jsonPath("$.subjects.length()").value(2))
                .andExpect(jsonPath("$.parents[0].firstName").value("Amina"))
                .andReturn().getResponse().getContentAsString();
        String studentId = objectMapper.readTree(createResponse).get("id").asText();

        // Findable by parent's name via the free-text search filter.
        mockMvc.perform(get("/api/students")
                        .header("Authorization", authHeader())
                        .param("search", "BenAli"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(studentId));

        // Filterable by subject and status.
        mockMvc.perform(get("/api/students")
                        .header("Authorization", authHeader())
                        .param("subject", "ENGLISH")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        // Not returned once filtered to a status it doesn't have.
        mockMvc.perform(get("/api/students")
                        .header("Authorization", authHeader())
                        .param("status", "WITHDRAWN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        ObjectNode updateBody = createBody.deepCopy();
        updateBody.put("status", "PAUSED");
        mockMvc.perform(put("/api/students/" + studentId)
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"));

        mockMvc.perform(delete("/api/students/" + studentId)
                        .header("Authorization", authHeader()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/students/" + studentId)
                        .header("Authorization", authHeader()))
                .andExpect(status().isNotFound());
    }

    @Test
    void creatingStudentWithUnknownParentIdIsRejected() throws Exception {
        ObjectNode createBody = objectMapper.createObjectNode();
        createBody.put("firstName", "Sara");
        createBody.put("lastName", "Trabelsi");
        createBody.put("enrollmentDate", "2026-09-01");
        createBody.put("status", "ACTIVE");
        createBody.putArray("parentIds").add(UUID.randomUUID().toString());

        mockMvc.perform(post("/api/students")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatesAndSearchesParents() throws Exception {
        String parentId = createParent("Fatma", "Zaidi");

        ObjectNode updateBody = objectMapper.createObjectNode();
        updateBody.put("firstName", "Fatma");
        updateBody.put("lastName", "Zaidi");
        updateBody.put("phone", "555-0199");
        updateBody.put("relationship", "Guardian");
        updateBody.put("preferredContactMethod", "WHATSAPP");

        mockMvc.perform(put("/api/parents/" + parentId)
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("555-0199"))
                .andExpect(jsonPath("$.relationship").value("Guardian"));

        mockMvc.perform(get("/api/parents")
                        .header("Authorization", authHeader())
                        .param("search", "Zaidi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(parentId));
    }

    @Test
    void creatingStudentWithUnknownTeacherIdIsRejected() throws Exception {
        ObjectNode createBody = objectMapper.createObjectNode();
        createBody.put("firstName", "Yassine");
        createBody.put("lastName", "Gharbi");
        createBody.put("enrollmentDate", "2026-09-01");
        createBody.put("status", "ACTIVE");
        createBody.put("assignedTeacherId", UUID.randomUUID().toString());

        mockMvc.perform(post("/api/students")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isBadRequest());
    }
}
