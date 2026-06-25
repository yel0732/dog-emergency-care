package com.ssafy.rescuemungz.user;

import com.ssafy.rescuemungz.auth.JwtTokenProvider;
import com.ssafy.rescuemungz.auth.JwtTokenBlacklist;
import com.ssafy.rescuemungz.auth.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final JwtTokenProvider tokenProvider;
    private final JwtTokenBlacklist tokenBlacklist;

    public AuthController(UserService userService, JwtTokenProvider tokenProvider, JwtTokenBlacklist tokenBlacklist) {
        this.userService = userService;
        this.tokenProvider = tokenProvider;
        this.tokenBlacklist = tokenBlacklist;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        UserResponse user = userService.login(request);
        return new AuthResponse("Bearer", tokenProvider.createAccessToken(user), tokenProvider.expirationSeconds(), user);
    }

    @PostMapping("/logout")
    public void logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = bearerToken(authorization);
        if (token != null) {
            tokenBlacklist.revoke(token, tokenProvider.expiresAt(token));
        }
    }

    @GetMapping("/me")
    public UserResponse me() {
        return userService.find(AuthUtil.requiredUserId());
    }

    private String bearerToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        return null;
    }
}
