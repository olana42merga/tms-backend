package com.taskmanagement.repository;

import com.taskmanagement.entity.SubTask;
import com.taskmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubTaskRepository extends JpaRepository<SubTask, Long> {

    @Query("SELECT s FROM SubTask s WHERE s.task.id = :taskId")
    List<SubTask> findByTaskId(@Param("taskId") Long taskId);

    List<SubTask> findByAssignedTo(User user);

    @Query("SELECT s FROM SubTask s WHERE s.assignedTo.id = :userId")
    List<SubTask> findByAssignedToUserId(@Param("userId") Long userId);
}