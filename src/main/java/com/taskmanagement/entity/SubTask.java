package com.taskmanagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.taskmanagement.enums.Priority;
import com.taskmanagement.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sub_tasks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TaskStatus status = TaskStatus.NOT_STARTED;

    @Column(name = "deadline")
    private LocalDateTime deadline;

    @Builder.Default
    private Integer progress = 0;

    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    @JsonIgnore
    private Task task;

    @ManyToOne
    @JoinColumn(name = "assigned_to")
    @JsonIgnore
    private User assignedTo;

    @ManyToOne
    @JoinColumn(name = "created_by")
    @JsonIgnore
    private User createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ✅ NEW: track when the subtask was submitted
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null)
            status = TaskStatus.NOT_STARTED;
        if (priority == null)
            priority = Priority.MEDIUM;
        if (progress == null)
            progress = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}