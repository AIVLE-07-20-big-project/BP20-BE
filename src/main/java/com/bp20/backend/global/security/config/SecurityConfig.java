package com.bp20.backend.global.security.config;

import com.bp20.backend.api.auth.session.RefreshTokenProperties;
import com.bp20.backend.global.security.authorization.Permission;
import com.bp20.backend.global.security.filter.InternalApiKeyFilter;
import com.bp20.backend.global.security.filter.JwtAuthenticationFilter;
import com.bp20.backend.global.security.handler.JsonAccessDeniedHandler;
import com.bp20.backend.global.security.handler.JsonAuthenticationEntryPoint;
import com.bp20.backend.global.security.jwt.JwtProperties;
import com.bp20.backend.global.security.account.AccountSecurityProperties;
import com.bp20.backend.global.security.captcha.CaptchaProperties;
import com.bp20.backend.global.security.crypto.PersonalDataEncryptionProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
@EnableConfigurationProperties({
        JwtProperties.class,
        RefreshTokenProperties.class,
        AccountSecurityProperties.class,
        CaptchaProperties.class,
        PersonalDataEncryptionProperties.class
})
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final InternalApiKeyFilter internalApiKeyFilter;
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
                                "/api/auth/token/refresh",
                                "/api/auth/logout",
                                "/api/internal/**",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/api/public/product-images/**",
                                // 로컬 디스크에 저장된 상품 이미지 정적 서빙 - <img> 태그는 Authorization 헤더를
                                // 못 보내므로 공개 접근을 허용한다 (S3를 쓰면 이 경로 자체가 안 쓰인다).
                                "/product-images/**",
                                "/api/notices/**"
                        ).permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/api/iam/invitation/store-owner")
                            .hasAuthority(Permission.ADMIN_MANAGE.name())
                            .requestMatchers(HttpMethod.GET, "/api/iam/invitation")
                            .hasAuthority(Permission.ADMIN_MANAGE.name())
                            .requestMatchers(HttpMethod.PATCH, "/api/iam/invitation/*/revoke")
                            .hasAuthority(Permission.ADMIN_MANAGE.name())
                            .requestMatchers("/api/iam/**").hasAuthority(Permission.IAM_ADMIN_MANAGE.name())
                            .requestMatchers("/api/admin/iam/**").hasAuthority(Permission.IAM_ADMIN_MANAGE.name())
                            .requestMatchers("/api/admin/**").hasAuthority(Permission.ADMIN_MANAGE.name())
                            .requestMatchers("/api/effect-verifications/**")
                            .hasAuthority(Permission.STORE_OWNER_ACCESS.name())
                            .requestMatchers("/api/store-owner/**").hasAuthority(Permission.STORE_OWNER_ACCESS.name())
                            .anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(internalApiKeyFilter, JwtAuthenticationFilter.class)
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
