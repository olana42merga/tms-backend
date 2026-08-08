package com.taskmanagement.repository;

import com.taskmanagement.entity.Task;
import com.taskmanagement.entity.User;
import com.taskmanagement.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByAssignedTo(User user);

    List<Task> findByAssignedToAndStatus(User user, TaskStatus status);

    List<Task> findByDeadlineBeforeAndStatusNot(LocalDateTime deadline, TaskStatus status);

    List<Task> findByCreatedBy(User user);
}