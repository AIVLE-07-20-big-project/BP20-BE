package com.bp20.backend.global.security.config;

import com.bp20.backend.global.security.authorization.Permission;
import com.bp20.backend.global.security.filter.JwtAuthenticationFilter;
import com.bp20.backend.global.security.handler.JsonAccessDeniedHandler;
import com.bp20.backend.global.security.handler.JsonAuthenticationEntryPoint;
import com.bp20.backend.global.security.jwt.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JsonAuthenticationEntryPoint authenticationEntryPoint;
    private final JsonAccessDeniedHandler accessDeniedHandler;

    @Value("${effect-verification.mock-public-access:false}")
    private boolean effectVerificationMockPublicAccess;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> {
                    if (effectVerificationMockPublicAccess) {
                        auth.requestMatchers(
                                "/api/mock/**",
                                "/api/effect-verifications/**"
                        ).permitAll();
                    }
                    auth.requestMatchers(
                                "/api/auth/login",
                                "/api/auth/signup",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll();
                    auth.requestMatchers("/api/iam/invitation/store-owner")
                            .hasAuthority(Permission.ADMIN_MANAGE.name())
                            .requestMatchers("/api/iam/**").hasAuthority(Permission.IAM_ADMIN_MANAGE.name())
                            .requestMatchers("/api/admin/iam/**").hasAuthority(Permission.IAM_ADMIN_MANAGE.name())
                            .requestMatchers("/api/admin/**").hasAuthority(Permission.ADMIN_MANAGE.name())
                            .requestMatchers("/api/store-owner/**").hasAuthority(Permission.STORE_OWNER_ACCESS.name())
                            .anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration
    ) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
