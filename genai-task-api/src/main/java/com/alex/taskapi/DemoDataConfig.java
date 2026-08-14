package com.alex.taskapi;

import com.alex.taskapi.auth.AppUser;
import com.alex.taskapi.auth.UserRepository;
import com.alex.taskapi.task.Task;
import com.alex.taskapi.task.TaskRepository;
import com.alex.taskapi.task.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DemoDataConfig {

    private static final String DEMO_EMAIL = "demo@example.com";
    private static final String DEMO_PASSWORD = "demo1234";

    @Bean
    CommandLineRunner seedDemoData(
            UserRepository userRepository,
            TaskRepository taskRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            AppUser demoUser =
                    userRepository
                            .findByEmail(DEMO_EMAIL)
                            .orElseGet(
                                    () -> {
                                        AppUser user = new AppUser();
                                        user.setEmail(DEMO_EMAIL);
                                        user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
                                        return userRepository.save(user);
                                    });

            if (taskRepository.countByOwnerEmail(demoUser.getEmail()) == 0) {
                Instant now = Instant.now();

                Task firstTask = new Task();
                firstTask.setTitle("Review pull request");
                firstTask.setDescription("Review the latest API changes");
                firstTask.setStatus(TaskStatus.TODO);
                firstTask.setDueDate(LocalDate.now().plusDays(3));
                firstTask.setOwnerEmail(demoUser.getEmail());
                firstTask.setCreatedAt(now);
                firstTask.setUpdatedAt(now);

                Task secondTask = new Task();
                secondTask.setTitle("Write integration tests");
                secondTask.setDescription("Cover task CRUD flows");
                secondTask.setStatus(TaskStatus.IN_PROGRESS);
                secondTask.setDueDate(LocalDate.now().plusDays(7));
                secondTask.setOwnerEmail(demoUser.getEmail());
                secondTask.setCreatedAt(now);
                secondTask.setUpdatedAt(now);

                taskRepository.save(firstTask);
                taskRepository.save(secondTask);
            }
        };
    }
}
