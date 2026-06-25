package com.ssafy.rescuemungz.auth;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtTokenBlacklist {
    private final Map<String, Long> revokedTokens = new ConcurrentHashMap<>();

    public void revoke(String token, long expiresAtEpochSecond) {
        cleanup();
        revokedTokens.put(token, expiresAtEpochSecond);
    }

    public boolean isRevoked(String token) {
        Long expiresAt = revokedTokens.get(token);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt <= Instant.now().getEpochSecond()) {
            revokedTokens.remove(token);
            return false;
        }
        return true;
    }

    private void cleanup() {
        long now = Instant.now().getEpochSecond();
        revokedTokens.entrySet().removeIf(entry -> entry.getValue() <= now);
    }
}
