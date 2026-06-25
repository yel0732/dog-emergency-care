package com.ssafy.rescuemungz.auth;

public record AuthUserPrincipal(
        Long userId,
        String loginId,
        String role
) {
}
