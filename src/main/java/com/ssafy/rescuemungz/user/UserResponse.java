package com.ssafy.rescuemungz.user;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String loginId,
        String name,
        String email,
        String nickname,
        String profileImageUrl,
        String role,
        Boolean active,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getLoginId(), user.getName(), user.getEmail(),
                user.getNickname(), user.getProfileImageUrl(), user.getRole(),
                user.getActive(), user.getCreatedAt());
    }
}
