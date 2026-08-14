package com.alex.taskapi.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.alex.taskapi.auth.JwtAuthFilter;
import com.alex.taskapi.auth.JwtService;
import com.alex.taskapi.auth.RestAuthenticationEntryPoint;
import com.alex.taskapi.auth.SecurityConfig;
import com.alex.taskapi.common.GlobalExceptionHandler;
import com.alex.taskapi.common.NotFoundException;
import com.alex.taskapi.task.dto.TaskPageResponse;
import com.alex.taskapi.task.dto.TaskResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaskController.class)
@Import({
    GlobalExceptionHandler.class,
    SecurityConfig.class,
    JwtAuthFilter.class,
    RestAuthenticationEntryPoint.class
})
class TaskControllerTest {

    private static final String VALID_TOKEN = "valid-token";

    @Autowired private MockMvc mockMvc;

    @MockBean private TaskService taskService;

    @MockBean private JwtService jwtService;

    @BeforeEach
    void setUpJwtParser() {
        when(jwtService.parseEmail(VALID_TOKEN)).thenReturn("user@example.com");
    }

    @Test
    void list_returnsTasks() throws Exception {
        TaskResponse task =
                new TaskResponse(
                        1L,
                        "Task",
                        "Desc",
                        TaskStatus.TODO,
                        LocalDate.now().plusDays(1),
                        "user@example.com",
                        Instant.parse("2026-01-01T00:00:00Z"),
                        Instant.parse("2026-01-01T00:00:00Z"),
                        null);
        when(taskService.list("user@example.com", 0, 20))
                .thenReturn(new TaskPageResponse(List.of(task), 0, 20, 1, 1));

        mockMvc.perform(get("/tasks").header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Task"));
    }

    @Test
    void create_invalidBody_returns400() throws Exception {
        mockMvc.perform(
                        post("/tasks")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"title\":\"\",\"status\":\"TODO\",\"dueDate\":\"2020-01-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(taskService.getById(eq("user@example.com"), eq(99L)))
                .thenThrow(new NotFoundException("Task not found: 99"));

        mockMvc.perform(get("/tasks/99").header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void create_returns201() throws Exception {
        TaskResponse created =
                new TaskResponse(
                        1L,
                        "New task",
                        null,
                        TaskStatus.TODO,
                        LocalDate.now().plusDays(2),
                        "user@example.com",
                        Instant.parse("2026-01-01T00:00:00Z"),
                        Instant.parse("2026-01-01T00:00:00Z"),
                        null);
        when(taskService.create(eq("user@example.com"), any())).thenReturn(created);

        mockMvc.perform(
                        post("/tasks")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "title": "New task",
                                          "status": "TODO",
                                          "dueDate": "%s"
                                        }
                                        """
                                                .formatted(LocalDate.now().plusDays(2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("New task"));
    }
}
