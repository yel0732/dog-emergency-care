package com.ssafy.rescuemungz.auth;

import com.ssafy.rescuemungz.user.User;
import com.ssafy.rescuemungz.user.UserMapper;
import com.ssafy.rescuemungz.common.UnauthorizedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider tokenProvider;
    private final JwtTokenBlacklist tokenBlacklist;
    private final UserMapper userMapper;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, JwtTokenBlacklist tokenBlacklist, UserMapper userMapper) {
        this.tokenProvider = tokenProvider;
        this.tokenBlacklist = tokenBlacklist;
        this.userMapper = userMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                if (tokenBlacklist.isRevoked(token)) {
                    throw new UnauthorizedException("Token has been revoked.");
                }
                AuthUserPrincipal principal = tokenProvider.parse(token);
                User user = userMapper.findById(principal.userId());
                if (user != null && Boolean.TRUE.equals(user.getActive())) {
                    String role = user.getRole() == null ? "USER" : user.getRole();
                    var authority = new SimpleGrantedAuthority("ROLE_" + role);
                    var authenticatedUser = new AuthUserPrincipal(user.getId(), user.getLoginId(), role);
                    var authentication = new UsernamePasswordAuthenticationToken(authenticatedUser, null, List.of(authority));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (UnauthorizedException ex) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        return null;
    }
}
