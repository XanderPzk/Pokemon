package com.alex.taskapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class TaskApiEndToEndTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @Test
    void registerLoginCrudAndCrossUserIsolation() throws Exception {
        String userOneToken = registerAndLogin("user-one@example.com", "password123");
        String userTwoToken = registerAndLogin("user-two@example.com", "password123");

        MvcResult createResult =
                mockMvc.perform(
                                post("/tasks")
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userOneToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "title": "Ship feature",
                                                  "description": "Finish task API",
                                                  "status": "TODO",
                                                  "dueDate": "%s"
                                                }
                                                """
                                                        .formatted(LocalDate.now().plusDays(5))))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.title").value("Ship feature"))
                        .andExpect(jsonPath("$.ownerEmail").value("user-one@example.com"))
                        .andReturn();

        long taskId =
                objectMapper
                        .readTree(createResult.getResponse().getContentAsString())
                        .get("id")
                        .asLong();

        mockMvc.perform(
                        get("/tasks/" + taskId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userOneToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId));

        mockMvc.perform(get("/tasks").header(HttpHeaders.AUTHORIZATION, "Bearer " + userOneToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(
                        put("/tasks/" + taskId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userOneToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "title": "Ship feature",
                                          "description": "Done",
                                          "status": "DONE",
                                          "dueDate": "%s"
                                        }
                                        """
                                                .formatted(LocalDate.now().plusDays(5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.completedAt").exists());

        mockMvc.perform(
                        get("/tasks/" + taskId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userTwoToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));

        mockMvc.perform(
                        delete("/tasks/" + taskId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userOneToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        get("/tasks/" + taskId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userOneToken))
                .andExpect(status().isNotFound());
    }

    private String registerAndLogin(String email, String password) throws Exception {
        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"email":"%s","password":"%s"}
                                        """
                                                .formatted(email, password)))
                .andExpect(status().isCreated());

        MvcResult loginResult =
                mockMvc.perform(
                                post("/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"email":"%s","password":"%s"}
                                                """
                                                        .formatted(email, password)))
                        .andExpect(status().isOk())
                        .andReturn();

        JsonNode body = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return body.get("token").asText();
    }
}
