package com.ssafy.rescuemungz.config;

import com.ssafy.rescuemungz.auth.JwtAuthenticationFilter;
import com.ssafy.rescuemungz.user.User;
import com.ssafy.rescuemungz.user.UserMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private static final int BCRYPT_STRENGTH = 12;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserMapper userMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, UserMapper userMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userMapper = userMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) -> response.sendError(HttpServletResponse.SC_FORBIDDEN)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/users").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/availability").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/emergency-videos/*/bookmark").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/emergency-videos/*/bookmark").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/emergency-videos").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/emergency-videos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/emergency-videos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/emergency-videos").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/emergency-videos/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/emergency-videos/*/reviews").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/video-reviews/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/hospitals/geocode").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/hospitals/sync-hours").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/hospitals/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/food-safety/**").permitAll()
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/",
                                "/login",
                                "/users",
                                "/follow",
                                "/followers",
                                "/following",
                                "/pets",
                                "/records",
                                "/videos",
                                "/emergency",
                                "/cases",
                                "/reports",
                                "/hospitals",
                                "/food-safety",
                                "/assets/**",
                                "/logo.png",
                                "/favicon.ico")
                        .permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            User user = userMapper.findByLoginId(username);
            if (user == null || !Boolean.TRUE.equals(user.getActive())) {
                throw new UsernameNotFoundException("User not found.");
            }
            String role = user.getRole() == null ? "USER" : user.getRole();
            return org.springframework.security.core.userdetails.User
                    .withUsername(user.getLoginId())
                    .password(user.getPasswordHash())
                    .roles(role)
                    .build();
        };
    }
}
