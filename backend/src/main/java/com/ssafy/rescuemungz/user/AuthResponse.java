package com.ssafy.rescuemungz.user;

public record AuthResponse(
        String tokenType,
        String accessToken,
        long expiresIn,
        UserResponse user
) {
}
