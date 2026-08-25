package com.taskmanagement.repository;

import com.taskmanagement.entity.Report;
import com.taskmanagement.entity.User;
import com.taskmanagement.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findBySubmittedBy(User user);

    @Query("SELECT r FROM Report r WHERE r.task.id = :taskId")
    List<Report> findByTaskId(@Param("taskId") Long taskId);

    List<Report> findByStatus(ReportStatus status);

    // ✅ NEW: find reports sent to a specific recipient
    List<Report> findByReportedTo(User recipient);
}