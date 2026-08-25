package com.taskmanagement.dto;

import com.taskmanagement.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String name;
    private String phone;
    private String role;
    private Boolean isActive;
    private String profileImage; // ✅ NEW - stores base64 image or URL

    public static UserResponse fromUser(User user) {
        if (user == null)
            return null;
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .isActive(user.getIsActive())
                .profileImage(user.getProfileImage()) // ✅ NEW
                .build();
    }
}