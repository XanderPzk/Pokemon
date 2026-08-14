package com.alex.taskapi.task;

import com.alex.taskapi.task.dto.CreateTaskRequest;
import com.alex.taskapi.task.dto.TaskPageResponse;
import com.alex.taskapi.task.dto.TaskResponse;
import com.alex.taskapi.task.dto.UpdateTaskRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public TaskPageResponse list(
            @AuthenticationPrincipal String ownerEmail,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return taskService.list(ownerEmail, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(
            @AuthenticationPrincipal String ownerEmail,
            @Valid @RequestBody CreateTaskRequest request) {
        return taskService.create(ownerEmail, request);
    }

    @GetMapping("/{id}")
    public TaskResponse getById(@AuthenticationPrincipal String ownerEmail, @PathVariable Long id) {
        return taskService.getById(ownerEmail, id);
    }

    @PutMapping("/{id}")
    public TaskResponse update(
            @AuthenticationPrincipal String ownerEmail,
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.update(ownerEmail, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal String ownerEmail, @PathVariable Long id) {
        taskService.delete(ownerEmail, id);
    }
}
