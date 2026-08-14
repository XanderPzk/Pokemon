package com.alex.taskapi.task;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Page<Task> findByOwnerEmailOrderByCreatedAtDesc(String ownerEmail, Pageable pageable);

    Optional<Task> findByIdAndOwnerEmail(Long id, String ownerEmail);

    long countByOwnerEmail(String ownerEmail);
}
