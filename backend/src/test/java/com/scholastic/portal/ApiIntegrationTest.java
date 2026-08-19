package com.scholastic.portal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * End-to-end API integration tests exercising the real JWT + role-guard pipeline
 * against an in-memory H2 database (seeded by the app's {@code DataSeeder}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:itest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.jwt.secret=test-secret-that-is-at-least-32-chars-long-for-hmac",
})
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

    private String login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();
        JsonNode node = json.readTree(result.getResponse().getContentAsString());
        return node.get("token").asText();
    }

    @Test
    @Order(1)
    void loginRejectsBadCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"teacher\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(2)
    void teacherCanSeeBookCatalog() throws Exception {
        String token = login("teacher");
        mockMvc.perform(get("/api/teacher/books").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].title").value("The Great Gatsby"));
    }

    @Test
    @Order(3)
    void teacherCreatesAssignmentAcrossAllStudents() throws Exception {
        String token = login("teacher");
        mockMvc.perform(post("/api/teacher/assignments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":2,\"dueDate\":\"2026-09-15\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedStudentsCount").value(3))
                .andExpect(jsonPath("$.notStartedCount").value(3));
    }

    @Test
    @Order(4)
    void studentSeesAssignedReadingAndUpdatesProgress() throws Exception {
        String token = login("student1");

        mockMvc.perform(get("/api/student/assignments").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("NOT_STARTED"));

        mockMvc.perform(get("/api/student/assignments/1").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isNotEmpty());

        // Mark in progress with 7 minutes read.
        mockMvc.perform(put("/api/student/assignments/1/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\",\"elapsedMinutes\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.elapsedMinutes").value(7));

        // Minutes are monotonic: a stale payload cannot regress stored value.
        mockMvc.perform(put("/api/student/assignments/1/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\",\"elapsedMinutes\":3}"))
                .andExpect(jsonPath("$.elapsedMinutes").value(7));
    }

    @Test
    @Order(5)
    void teacherSeesStudentProgress() throws Exception {
        String token = login("teacher");
        mockMvc.perform(get("/api/teacher/assignments/1/progress").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentProgress[0].studentName").value("Ava"))
                .andExpect(jsonPath("$.studentProgress[0].status").value("IN_PROGRESS"));
    }

    @Test
    @Order(6)
    void roleGuardsAreEnforced() throws Exception {
        String studentToken = login("student1");
        String teacherToken = login("teacher");

        mockMvc.perform(get("/api/teacher/books").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/student/assignments").header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/teacher/books")).andExpect(status().isForbidden());
    }

    /**
     * Regression: the student-facing id is the ASSIGNMENT id, not the progress-row id.
     * (Row ids and assignment ids diverge once a second assignment exists.)
     */
    @Test
    @Order(7)
    void studentUsesAssignmentIdNotProgressRowId() throws Exception {
        String teacherToken = login("teacher");
        String studentToken = login("student1");

        mockMvc.perform(post("/api/teacher/assignments")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":3,\"dueDate\":\"2026-11-01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));

        // Student list exposes the assignment id (2), not the progress-row id (4).
        mockMvc.perform(get("/api/student/assignments").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.bookTitle == 'A Tale of Two Cities')].id").value(2));

        // Detail + status update address the assignment by that id.
        mockMvc.perform(get("/api/student/assignments/2").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookTitle").value("A Tale of Two Cities"));

        mockMvc.perform(put("/api/student/assignments/2/status")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\",\"elapsedMinutes\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
    }
}