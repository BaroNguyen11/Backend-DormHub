
package com.dormhub.config;

import com.dormhub.dto.response.ApiResponse;
import com.dormhub.repository.UserRepository;
import com.dormhub.security.JwtAuthenticationFilter;
import com.dormhub.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${dormhub.cors.allowed-origins:http://localhost:5173,http://localhost:3000,https://dorm-hub-s8y9.vercel.app}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService, userRepository);
        http.cors(cors ->
                        cors.configurationSource(corsConfigurationSource())
                )
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers(
                                PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/v1/payments/webhooks/sepay"),
                                PathPatternRequestMatcher.withDefaults().matcher("/api/v1/auth/**")
                        )
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                .exceptionHandling(exception -> exception

                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");

                            objectMapper.writeValue(
                                    response.getOutputStream(),
                                    ApiResponse.error(
                                            "UNAUTHORIZED",
                                            "Bạn cần đăng nhập để truy cập tài nguyên này"
                                    )
                            );
                        })

                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");

                            objectMapper.writeValue(
                                    response.getOutputStream(),
                                    ApiResponse.error(
                                            "ACCESS_DENIED",
                                            "Bạn không có quyền truy cập tài nguyên này"
                                    )
                            );
                        })
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/webhooks/sepay").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                        "/api/v1/auth/login",
                                        "/api/v1/auth/verify-email",
                                        "/api/v1/auth/resend-otp",
                                        "/api/v1/auth/reset-password",
                                        "/api/v1/auth/forgot-password",
                                        "/api/v1/auth/logout",
                                        "/api/v1/auth/refresh"
                                ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/rooms/available", "/api/v1/rooms/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/change-password").hasAnyRole("STUDENT", "MANAGER")
                        .requestMatchers("/api/v1/invoices/**", "/api/v1/room-monthly-charges/**").hasRole("MANAGER")
                        .requestMatchers("/api/v1/manager/**").hasRole("MANAGER")
                        .requestMatchers("/api/v1/contracts", "/api/v1/contracts/**").hasRole("MANAGER")
                        .requestMatchers("/api/v1/student/**").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/notifications/manager-history",
                                "/api/v1/notifications/manager-history/**").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/notifications/send-to-students").hasRole("MANAGER")
                        .requestMatchers("/api/v1/notifications/**").authenticated()
                        .anyRequest().authenticated()
                        )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> normalizedOrigins = allowedOrigins.stream()
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .map(origin -> origin.replaceAll("/+$", ""))
                .toList();
        configuration.setAllowedOriginPatterns(normalizedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
