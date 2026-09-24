package com.ems;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end through every layer (filter chain -> controller -> service -> DAO -> H2)
 * using real JWTs issued by the login endpoint.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EmployeeApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(body).get("token").asText();
    }

    @Test
    void wrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void tamperedToken_returns401() throws Exception {
        // A USER edits the token payload to impersonate "admin" but cannot re-sign it
        String[] parts = login("user", "user123").substring("Bearer ".length()).split("\\.");
        Base64.Decoder decoder = Base64.getUrlDecoder();
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String forgedPayload = new String(decoder.decode(parts[1]), StandardCharsets.UTF_8)
                .replace("\"sub\":\"user\"", "\"sub\":\"admin\"");
        String forged = parts[0] + "." + encoder.encodeToString(forgedPayload.getBytes(StandardCharsets.UTF_8))
                + "." + parts[2];

        mockMvc.perform(get("/api/employees").header(HttpHeaders.AUTHORIZATION, "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminFullCrudFlow() throws Exception {
        String admin = login("admin", "admin123");
        String employee = """
                {"firstName":"Test","lastName":"Person","email":"test.person@company.com",
                 "department":"QA","designation":"Tester","salary":500000,"dateOfJoining":"2024-05-01"}
                """;

        // Create
        String created = mockMvc.perform(post("/api/employees")
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content(employee))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).get("id").asLong();

        // Duplicate email is a conflict
        mockMvc.perform(post("/api/employees")
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content(employee))
                .andExpect(status().isConflict());

        // Update
        mockMvc.perform(put("/api/employees/" + id)
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employee.replace("Tester", "Senior Tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.designation").value("Senior Tester"));

        // Search finds it
        mockMvc.perform(get("/api/employees").param("search", "test.person")
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        // Delete, then it is gone
        mockMvc.perform(delete("/api/employees/" + id).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/employees/" + id).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void userIsReadOnly() throws Exception {
        String user = login("user", "user123");

        JsonNode page = objectMapper.readTree(mockMvc.perform(get("/api/employees")
                        .param("sortBy", "salary").param("direction", "desc")
                        .header(HttpHeaders.AUTHORIZATION, user))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        long firstId = page.get("content").get(0).get("id").asLong();

        mockMvc.perform(delete("/api/employees/" + firstId).header(HttpHeaders.AUTHORIZATION, user))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownSortField_returns400() throws Exception {
        String user = login("user", "user123");
        mockMvc.perform(get("/api/employees").param("sortBy", "password")
                        .header(HttpHeaders.AUTHORIZATION, user))
                .andExpect(status().isBadRequest());
    }
}
