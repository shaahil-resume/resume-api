package com.shaahil.resumeapi.auth.config;

import com.shaahil.resumeapi.auth.AdminOidcUserService;
import com.shaahil.resumeapi.auth.repository.AdminUserRepository;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;

/**
 * Central security configuration for the auth module — holds the beans
 * shared across local login, the OAuth2 Authorization Server, and the admin
 * dashboard, plus (later) the main-app SecurityFilterChain.
 */
@Configuration
public class SecurityConfig {

    /**
     * Method 1: the PasswordEncoder used everywhere a password or client
     * secret needs to be hashed or verified — BCrypt, Spring Security's
     * standard choice (salted, adaptive cost). AdminUser hashes its password
     * with this before it's saved; AuthorizationServerConfig will hash the
     * OAuth2 client secret with it too.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Method 2: loads an admin by username for Spring Security's
     * authentication machinery. Fetches the row from admin_users via
     * AdminUserRepository, then wraps it as a Spring Security User exposing
     * the already-BCrypt-hashed password and enabled flag —
     * DaoAuthenticationProvider compares that hash against whatever the
     * login form submitted (re-hashed with the same passwordEncoder).
     */
    @Bean
    public UserDetailsService userDetailsService(AdminUserRepository adminUserRepository) {
        return username -> adminUserRepository.findByUsername(username)
                .map(admin -> User.withUsername(admin.getUsername())
                        .password(admin.getPassword())
                        .roles("ADMIN")
                        .disabled(!admin.isEnabled())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("No admin found: " + username));
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // Used by pitcher.
                        .requestMatchers(HttpMethod.GET, "/api/resume/**")
                        .hasAuthority("SCOPE_resume:read")
                        // Used by bff.
                        .requestMatchers(HttpMethod.POST, "/api/contact")
                        .hasAuthority("SCOPE_contact:submit")
                        // Used by ai-module.
                        .requestMatchers(HttpMethod.GET, "/api/profile/export")
                        .hasAuthority("SCOPE_profile:export")
                        .requestMatchers(HttpMethod.POST, "/api/ai/chat")
                        .hasAuthority("SCOPE_ai:chat")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(Customizer.withDefaults()));
        return http.build();
    }

    /**
     * Method 3: the main SecurityFilterChain, @Order(3) so
     * AuthorizationServerConfig's OAuth2/OIDC chain (@Order(1)) gets first
     * look at any request matching its endpoints. These are two independent
     * chains — only one runs per request, picked by the URL — linked by a
     * redirect round-trip: when an unauthenticated request hits an OAuth2
     * endpoint, AuthorizationServerConfig's chain redirects to /login, which
     * this chain catches. Two ways to authenticate from there: formLogin()
     * (username/password, checked via userDetailsService()/passwordEncoder()
     * through Spring's auto-configured DaoAuthenticationProvider) or
     * oauth2Login() (redirects to Google using GoogleClientConfig's
     * ClientRegistrationRepository, then maps the verified identity to an
     * AdminUser). Either way, once authenticated the browser is redirected
     * back to the original OAuth2 request, which AuthorizationServerConfig's
     * chain now sees as authenticated.
     */
    @Bean
    @Order(3)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, AdminOidcUserService adminOidcUserService) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login", "/error").permitAll()
                        .anyRequest().authenticated())
                .formLogin(Customizer.withDefaults())
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(adminOidcUserService)));
        return http.build();
    }

}
