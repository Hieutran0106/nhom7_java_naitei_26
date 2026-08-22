package com.nhom7.coworkingspace.config;

import com.nhom7.coworkingspace.security.CustomUserDetailsService;
import com.nhom7.coworkingspace.security.JwtAuthErrorHandler;
import com.nhom7.coworkingspace.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthErrorHandler jwtAuthErrorHandler;

    @Value("${app.cors.allowed-origins}")
    private List<String> corsAllowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF only for REST API; keep enabled for Thymeleaf form submissions
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))

                // CORS must be configured at the Security layer, not only at the controller
                // level
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // IF_REQUIRED: create session only when needed (Thymeleaf needs it for CSRF
                // token)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

                // Never load/save the authenticated principal from/to the HTTP session.
                // Without this, Spring Security's default HttpSessionSecurityContextRepository
                // persists whoever JwtAuthenticationFilter authenticates into the session and
                // silently restores them on later requests via the JSESSIONID cookie alone -
                // bypassing the JWT check entirely (a logged-out/blacklisted/missing token would
                // still "work" as long as the old session cookie is sent). Every request must
                // prove identity via its own Authorization header.
                .securityContext(securityContext ->
                        securityContext.securityContextRepository(new NullSecurityContextRepository()))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/api/auth/**",
                                "/error",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                // OpenAPI specification
                                "/v3/api-docs/**",
                                "/api-docs/**")
                        .permitAll()

                        // REST API dành cho Admin
                         .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Thymeleaf web UI authorization
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/moderator/**").hasAnyRole("ADMIN", "MODERATOR")

                        .anyRequest().authenticated())

                // Without this, a missing/blacklisted/refresh token on a protected endpoint is
                // rejected before DispatcherServlet ever runs, so GlobalExceptionHandler never
                // sees it - the caller gets Spring Boot's generic default error page instead of
                // a clear, localized ApiResponse message. Both callbacks are wired to the same
                // handler: a request with no credentials at all goes through
                // authenticationEntryPoint, one with credentials Spring Security still rejects
                // (e.g. role checks) goes through accessDeniedHandler.
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jwtAuthErrorHandler)
                        .accessDeniedHandler(jwtAuthErrorHandler))

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(corsAllowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }
}
