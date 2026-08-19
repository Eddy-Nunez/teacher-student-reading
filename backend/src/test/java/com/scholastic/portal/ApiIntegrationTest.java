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

import jakarta.servlet.http.Cookie;

/**
 * End-to-end API integration tests exercising the real JWT + cookie + role-guard pipeline
 * against an in-memory H2 database (seeded by {@code DataSeeder}).
 *
 * CSRF is disabled in the test profile (app.security.csrf-enabled=false) so these tests focus on
 * auth + authorization semantics; the CSRF double-submit flow is covered by browser E2E.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:itest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.jwt.secret=test-secret-that-is-at-least-32-chars-long-for-hmac",
        "app.security.csrf-enabled=false",
        "app.cookie.secure=false",
})
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private Cookie authCookie(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").doesNotExist()) // token never in the body
                .andReturn();
        return result.getResponse().getCookie("portal_token");
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
    void loginSetsHttpOnlyAuthCookieNotExposedInBody() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"teacher\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("teacher"))
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andReturn();
        String setCookie = result.getResponse().getHeader("Set-Cookie");
        org.assertj.core.api.Assertions.assertThat(setCookie)
                .contains("HttpOnly")
                .contains("SameSite=Lax")
                .contains("portal_token=");
    }

    @Test
    @Order(3)
    void teacherCanSeeBookCatalog() throws Exception {
        mockMvc.perform(get("/api/teacher/books").cookie(authCookie("teacher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].title").value("The Great Gatsby"));
    }

    @Test
    @Order(4)
    void teacherCreatesAssignmentAcrossAllStudents() throws Exception {
        mockMvc.perform(post("/api/teacher/assignments").cookie(authCookie("teacher"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":2,\"dueDate\":\"2026-09-15\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedStudentsCount").value(3))
                .andExpect(jsonPath("$.notStartedCount").value(3));
    }

    @Test
    @Order(5)
    void studentSeesAssignedReadingAndUpdatesProgress() throws Exception {
        Cookie token = authCookie("student1");

        mockMvc.perform(get("/api/student/assignments").cookie(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("NOT_STARTED"));

        mockMvc.perform(get("/api/student/assignments/1").cookie(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isNotEmpty());

        mockMvc.perform(put("/api/student/assignments/1/status").cookie(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\",\"elapsedMinutes\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.elapsedMinutes").value(7));

        // Minutes are monotonic: a stale payload cannot regress stored value.
        mockMvc.perform(put("/api/student/assignments/1/status").cookie(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\",\"elapsedMinutes\":3}"))
                .andExpect(jsonPath("$.elapsedMinutes").value(7));
    }

    @Test
    @Order(6)
    void teacherSeesStudentProgress() throws Exception {
        mockMvc.perform(get("/api/teacher/assignments/1/progress").cookie(authCookie("teacher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentProgress[0].studentName").value("Ava"))
                .andExpect(jsonPath("$.studentProgress[0].status").value("IN_PROGRESS"));
    }

    @Test
    @Order(7)
    void roleGuardsAreEnforced() throws Exception {
        Cookie studentToken = authCookie("student1");
        Cookie teacherToken = authCookie("teacher");

        mockMvc.perform(get("/api/teacher/books").cookie(studentToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/student/assignments").cookie(teacherToken))
                .andExpect(status().isForbidden());
        // No cookie at all → 401 (unauthenticated).
        mockMvc.perform(get("/api/teacher/books"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Regression: the student-facing id is the ASSIGNMENT id, not the progress-row id.
     */
    @Test
    @Order(8)
    void studentUsesAssignmentIdNotProgressRowId() throws Exception {
        Cookie studentToken = authCookie("student1");

        mockMvc.perform(post("/api/teacher/assignments").cookie(authCookie("teacher"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":3,\"dueDate\":\"2026-11-01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));

        mockMvc.perform(get("/api/student/assignments").cookie(studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.bookTitle == 'A Tale of Two Cities')].id").value(2));

        mockMvc.perform(get("/api/student/assignments/2").cookie(studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookTitle").value("A Tale of Two Cities"));

        mockMvc.perform(put("/api/student/assignments/2/status").cookie(studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\",\"elapsedMinutes\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
    }

    /**
     * Security: the auth cookie must be HttpOnly so the JWT cannot be read/exfiltrated by XSS.
     */
    @Test
    @Order(9)
    void csrfDisabledButSessionStillCookieBased() throws Exception {
        Cookie token = authCookie("student1");
        // /me resolves the session from the cookie alone (no header).
        mockMvc.perform(get("/api/auth/me").cookie(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("student1"));
    }

    /**
     * Concurrency regression: concurrent status PUTs must not lose updates. The pessimistic
     * row lock serializes the read-modify-write, so the final minutes equal the maximum sent,
     * never a value computed from a stale snapshot.
     */
    @Test
    @Order(10)
    void concurrentStatusUpdatesNeverLoseMinutes() throws Exception {
        Cookie token = authCookie("student1");
        int threads = 6;
        var pool = java.util.concurrent.Executors.newFixedThreadPool(threads);
        var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();
        // Distinct increasing values sent in parallel; final must be the max (never regressed).
        for (int i = 1; i <= threads; i++) {
            final int minutes = 20 + i;
            futures.add(pool.submit(() -> {
                try {
                    mockMvc.perform(put("/api/student/assignments/1/status").cookie(token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"status\":\"IN_PROGRESS\",\"elapsedMinutes\":" + minutes + "}"))
                            .andExpect(status().isOk());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }
        for (var f : futures) {
            f.get(10, java.util.concurrent.TimeUnit.SECONDS);
        }
        pool.shutdown();

        mockMvc.perform(get("/api/student/assignments/1").cookie(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elapsedMinutes").value(20 + threads));
    }
}