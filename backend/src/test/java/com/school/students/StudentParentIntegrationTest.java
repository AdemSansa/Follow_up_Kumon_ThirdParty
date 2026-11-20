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

import static org.assertj.core.api.Assertions.assertThat;
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
        String parentId = createParent("Amina", "Zqtestfamily");

        ObjectNode createBody = objectMapper.createObjectNode();
        createBody.put("firstName", "Ahmed");
        createBody.put("lastName", "Zqtestfamily");
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
                        .param("search", "Zqtestfamily"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(studentId));

        // Findable as one of this parent's children (parent-first
        // registration flow — see the "Add child" action on a parent).
        mockMvc.perform(get("/api/students")
                        .header("Authorization", authHeader())
                        .param("parentId", parentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(studentId));

        // Filterable by subject and status — scoped with the search term
        // too, since this runs against a shared dev DB that may already
        // hold other ACTIVE/ENGLISH students.
        mockMvc.perform(get("/api/students")
                        .header("Authorization", authHeader())
                        .param("search", "Zqtestfamily")
                        .param("subject", "ENGLISH")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(studentId));

        // Not returned once filtered to a status it doesn't have.
        mockMvc.perform(get("/api/students")
                        .header("Authorization", authHeader())
                        .param("search", "Zqtestfamily")
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
    void listsParentsWithoutASearchFilter() throws Exception {
        // Regression test: an unfiltered list must not NPE/500 — a null
        // search value previously made Hibernate mis-infer the JPQL
        // parameter type. See ParentRepository.search(). Doesn't assert an
        // exact count: this runs against a shared dev DB that may already
        // hold other parents.
        String parentId = createParent("Karim", "Haddad");

        String response = mockMvc.perform(get("/api/parents")
                        .header("Authorization", authHeader())
                        .param("size", "200"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(response).contains(parentId);
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

    private String createStudent(String firstName, String lastName) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("firstName", firstName);
        body.put("lastName", lastName);
        body.put("enrollmentDate", "2026-09-01");
        body.put("status", "ACTIVE");
        String response = mockMvc.perform(post("/api/students")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    @Test
    void studentPhotoCanBeUploadedReadAndRemovedAndOnlyRealImagesAreAccepted() throws Exception {
        String studentId = createStudent("Lina", "Photo");
        String url = "/api/students/" + studentId + "/photo";
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13, 'I', 'H', 'D', 'R'};

        mockMvc.perform(get(url).header("Authorization", authHeader()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(url)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/students/" + studentId).header("Authorization", authHeader()))
                .andExpect(jsonPath("$.photoUpdatedAt").doesNotExist());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart(url)
                        .file(new org.springframework.mock.web.MockMultipartFile("file", "face.png", "image/png", png))
                        .with(r -> { r.setMethod("PUT"); return r; })
                        .header("Authorization", authHeader()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(url).header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType("image/png"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().bytes(png));

        // The student and the list say a photo exists (so the UI only fetches images that exist).
        mockMvc.perform(get("/api/students/" + studentId).header("Authorization", authHeader()))
                .andExpect(jsonPath("$.photoUpdatedAt").isNotEmpty());
        mockMvc.perform(get("/api/students").param("search", "Photo").header("Authorization", authHeader()))
                .andExpect(jsonPath("$.content[?(@.id == '" + studentId + "')].photoUpdatedAt").isNotEmpty());

        // A script/SVG/text file renamed to .png is refused (type is checked from the bytes).
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart(url)
                        .file(new org.springframework.mock.web.MockMultipartFile("file", "x.png", "image/png",
                                "<svg onload=alert(1)>".getBytes()))
                        .with(r -> { r.setMethod("PUT"); return r; })
                        .header("Authorization", authHeader()))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete(url).header("Authorization", authHeader()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(url).header("Authorization", authHeader()))
                .andExpect(status().isNotFound());
    }

    @Test
    void staffCannotDeleteAStudent() throws Exception {
        String studentId = createStudent("Rania", "Mansour");

        mockMvc.perform(delete("/api/students/" + studentId)
                        .header("Authorization",
                                "Bearer " + jwtService.generateToken("staff-only@example.com", Role.STAFF)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/students/" + studentId)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk());
    }

    @Test
    void studentWithFollowUpHistoryCannotBeDeleted() throws Exception {
        // Business rule 1: follow-up history is never deleted.
        String studentId = createStudent("Omar", "Jaziri");

        ObjectNode followUp = objectMapper.createObjectNode();
        followUp.put("studentId", studentId);
        followUp.put("type", "PHONE_CALL");
        followUp.put("scheduledStart", "2026-09-10T10:00:00");
        followUp.put("scheduledEnd", "2026-09-10T10:30:00");
        mockMvc.perform(post("/api/follow-ups")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(followUp)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/students/" + studentId)
                        .header("Authorization", authHeader()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("WITHDRAWN")));

        mockMvc.perform(get("/api/follow-ups")
                        .header("Authorization", authHeader())
                        .param("studentId", studentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
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
