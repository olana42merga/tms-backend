package com.taskmanagement.repository;

import com.taskmanagement.entity.Notification;
import com.taskmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    List<Notification> findByUserAndIsReadFalseOrderByCreatedAtDesc(User user);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.isRead = false")
    Long countByUserIdAndIsReadFalse(@Param("userId") Long userId);

    List<Notification> findByUserIdAndIsReadFalse(Long userId);
}
