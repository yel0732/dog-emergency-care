package com.ssafy.rescuemungz.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.rescuemungz.common.UnauthorizedException;
import com.ssafy.rescuemungz.user.UserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class JwtTokenProvider {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long expirationSeconds;

    public JwtTokenProvider(
            ObjectMapper objectMapper,
            @Value("${auth.jwt.secret}") String secret,
            @Value("${auth.jwt.expiration-seconds:7200}") long expirationSeconds) {
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds;
    }

    public String createAccessToken(UserResponse user) {
        Instant now = Instant.now();
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", String.valueOf(user.id()));
        payload.put("loginId", user.loginId());
        payload.put("role", user.role() == null ? "USER" : user.role());
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", now.plusSeconds(expirationSeconds).getEpochSecond());
        String headerPart = encode(header);
        String payloadPart = encode(payload);
        String signedContent = headerPart + "." + payloadPart;
        return signedContent + "." + sign(signedContent);
    }

    public AuthUserPrincipal parse(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new UnauthorizedException("Invalid token.");
            }
            String signedContent = parts[0] + "." + parts[1];
            if (!constantTimeEquals(sign(signedContent), parts[2])) {
                throw new UnauthorizedException("Invalid token signature.");
            }
            Map<String, Object> payload = payload(parts[1]);
            long exp = number(payload.get("exp"));
            if (exp < Instant.now().getEpochSecond()) {
                throw new UnauthorizedException("Token expired.");
            }
            return new AuthUserPrincipal(
                    Long.valueOf(String.valueOf(payload.get("sub"))),
                    String.valueOf(payload.get("loginId")),
                    String.valueOf(payload.getOrDefault("role", "USER"))
            );
        } catch (UnauthorizedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UnauthorizedException("Invalid token.");
        }
    }

    public long expiresAt(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new UnauthorizedException("Invalid token.");
            }
            String signedContent = parts[0] + "." + parts[1];
            if (!constantTimeEquals(sign(signedContent), parts[2])) {
                throw new UnauthorizedException("Invalid token signature.");
            }
            return number(payload(parts[1]).get("exp"));
        } catch (UnauthorizedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UnauthorizedException("Invalid token.");
        }
    }

    public long expirationSeconds() {
        return expirationSeconds;
    }

    private String encode(Map<String, Object> value) {
        try {
            return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception ex) {
            throw new IllegalStateException("JWT payload serialization failed.", ex);
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return BASE64_URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("JWT signing failed.", ex);
        }
    }

    private long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private Map<String, Object> payload(String payloadPart) throws Exception {
        return objectMapper.readValue(BASE64_URL_DECODER.decode(payloadPart), MAP_TYPE);
    }

    private boolean constantTimeEquals(String expected, String actual) {
        byte[] left = expected.getBytes(StandardCharsets.UTF_8);
        byte[] right = actual.getBytes(StandardCharsets.UTF_8);
        if (left.length != right.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < left.length; i += 1) {
            diff |= left[i] ^ right[i];
        }
        return diff == 0;
    }
}
