package com.taskmanagement.entity;

import com.taskmanagement.enums.Role;
import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;
    private String name;
    private String phone;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // ✅ NEW: profile image (stores base64 or URL)
    @Column(name = "profile_image", columnDefinition = "TEXT")
    private String profileImage;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}