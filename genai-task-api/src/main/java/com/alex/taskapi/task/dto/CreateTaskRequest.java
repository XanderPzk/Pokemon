package com.alex.taskapi.task.dto;

import com.alex.taskapi.task.TaskStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateTaskRequest(
        @NotBlank String title,
        String description,
        @NotNull TaskStatus status,
        @FutureOrPresent LocalDate dueDate) {}
