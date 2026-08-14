package com.alex.taskapi.task;

import com.alex.taskapi.common.NotFoundException;
import com.alex.taskapi.task.dto.CreateTaskRequest;
import com.alex.taskapi.task.dto.TaskPageResponse;
import com.alex.taskapi.task.dto.TaskResponse;
import com.alex.taskapi.task.dto.UpdateTaskRequest;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional
    public TaskResponse create(String ownerEmail, CreateTaskRequest request) {
        Instant now = Instant.now();
        Task task = new Task();
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status());
        task.setDueDate(request.dueDate());
        task.setOwnerEmail(ownerEmail);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        applyCompletionTimestamp(task, request.status(), now);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public TaskPageResponse list(String ownerEmail, int page, int size) {
        Page<Task> taskPage =
                taskRepository.findByOwnerEmailOrderByCreatedAtDesc(
                        ownerEmail, PageRequest.of(page, size));
        return new TaskPageResponse(
                taskPage.getContent().stream().map(TaskResponse::from).toList(),
                taskPage.getNumber(),
                taskPage.getSize(),
                taskPage.getTotalElements(),
                taskPage.getTotalPages());
    }

    @Transactional(readOnly = true)
    public TaskResponse getById(String ownerEmail, Long id) {
        return TaskResponse.from(findOwnedTask(ownerEmail, id));
    }

    @Transactional
    public TaskResponse update(String ownerEmail, Long id, UpdateTaskRequest request) {
        Task task = findOwnedTask(ownerEmail, id);
        Instant now = Instant.now();
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status());
        task.setDueDate(request.dueDate());
        task.setUpdatedAt(now);
        applyCompletionTimestamp(task, request.status(), now);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public void delete(String ownerEmail, Long id) {
        Task task = findOwnedTask(ownerEmail, id);
        taskRepository.delete(task);
    }

    private Task findOwnedTask(String ownerEmail, Long id) {
        return taskRepository
                .findByIdAndOwnerEmail(id, ownerEmail)
                .orElseThrow(() -> new NotFoundException("Task not found: " + id));
    }

    private void applyCompletionTimestamp(Task task, TaskStatus status, Instant now) {
        if (status == TaskStatus.DONE) {
            if (task.getCompletedAt() == null) {
                task.setCompletedAt(now);
            }
        } else {
            task.setCompletedAt(null);
        }
    }
}
