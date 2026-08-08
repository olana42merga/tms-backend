package com.taskmanagement.repository;

import com.taskmanagement.entity.Report;
import com.taskmanagement.entity.User;
import com.taskmanagement.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findBySubmittedBy(User user);

    List<Report> findByTaskId(Long taskId);

    List<Report> findByStatus(ReportStatus status);
}