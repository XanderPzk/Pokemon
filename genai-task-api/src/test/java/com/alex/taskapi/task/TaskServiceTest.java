package com.alex.taskapi.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alex.taskapi.common.NotFoundException;
import com.alex.taskapi.task.dto.CreateTaskRequest;
import com.alex.taskapi.task.dto.UpdateTaskRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;

    @InjectMocks private TaskService taskService;

    @Test
    void create_setsOwnerAndCompletionTimestampWhenDone() {
        when(taskRepository.save(any(Task.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response =
                taskService.create(
                        "owner@example.com",
                        new CreateTaskRequest(
                                "Done task",
                                "details",
                                TaskStatus.DONE,
                                LocalDate.now().plusDays(1)));

        assertThat(response.ownerEmail()).isEqualTo("owner@example.com");
        assertThat(response.completedAt()).isNotNull();
    }

    @Test
    void getById_otherUsersTask_returns404() {
        when(taskRepository.findByIdAndOwnerEmail(1L, "owner@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getById("owner@example.com", 1L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void update_toDone_setsCompletedAt() {
        Task existing = new Task();
        existing.setId(1L);
        existing.setTitle("Old");
        existing.setStatus(TaskStatus.TODO);
        existing.setOwnerEmail("owner@example.com");
        existing.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        existing.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));

        when(taskRepository.findByIdAndOwnerEmail(1L, "owner@example.com"))
                .thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response =
                taskService.update(
                        "owner@example.com",
                        1L,
                        new UpdateTaskRequest(
                                "Updated", "desc", TaskStatus.DONE, LocalDate.now().plusDays(3)));

        assertThat(response.status()).isEqualTo(TaskStatus.DONE);
        assertThat(response.completedAt()).isNotNull();
    }

    @Test
    void delete_removesOwnedTask() {
        Task existing = new Task();
        existing.setId(1L);
        existing.setOwnerEmail("owner@example.com");

        when(taskRepository.findByIdAndOwnerEmail(1L, "owner@example.com"))
                .thenReturn(Optional.of(existing));

        taskService.delete("owner@example.com", 1L);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).delete(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1L);
    }

    @Test
    void list_returnsPagedTasks() {
        Task task = new Task();
        task.setId(1L);
        task.setTitle("Task");
        task.setStatus(TaskStatus.TODO);
        task.setOwnerEmail("owner@example.com");
        task.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        task.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));

        when(taskRepository.findByOwnerEmailOrderByCreatedAtDesc(
                        "owner@example.com", PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(java.util.List.of(task)));

        var page = taskService.list("owner@example.com", 0, 20);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).title()).isEqualTo("Task");
    }
}
