package com.taskmanagement.repository;

import com.taskmanagement.entity.SubTask;
import com.taskmanagement.entity.User;
import com.taskmanagement.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SubTaskRepository extends JpaRepository<SubTask, Long> {

    @Query("SELECT s FROM SubTask s WHERE s.task.id = :taskId")
    List<SubTask> findByTaskId(@Param("taskId") Long taskId);

    List<SubTask> findByAssignedTo(User user);

    @Query("SELECT s FROM SubTask s WHERE s.assignedTo.id = :userId")
    List<SubTask> findByAssignedToUserId(@Param("userId") Long userId);

    // ✅ NEW: Find subtasks that are overdue (deadline < now) and not yet completed
    // or submitted
    @Query("SELECT s FROM SubTask s WHERE s.deadline IS NOT NULL AND s.deadline < :now AND s.status NOT IN :excludedStatuses")
    List<SubTask> findByDeadlineBeforeAndStatusNotIn(
            @Param("now") LocalDateTime now,
            @Param("excludedStatuses") List<TaskStatus> excludedStatuses);
}