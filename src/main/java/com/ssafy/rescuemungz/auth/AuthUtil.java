package com.ssafy.rescuemungz.auth;

import com.ssafy.rescuemungz.common.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthUtil {
    private AuthUtil() {
    }

    public static long requiredUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUserPrincipal principal) {
            return principal.userId();
        }
        throw new UnauthorizedException("Login is required.");
    }

    public static long optionalUserIdOrZero() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUserPrincipal principal) {
            return principal.userId();
        }
        return 0L;
    }

    public static boolean currentUserIsAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUserPrincipal principal) {
            return "ADMIN".equalsIgnoreCase(principal.role());
        }
        return false;
    }
}
