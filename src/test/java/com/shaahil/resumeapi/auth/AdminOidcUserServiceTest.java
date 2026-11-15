package com.shaahil.resumeapi.auth;

import com.shaahil.resumeapi.auth.entity.AdminUser;
import com.shaahil.resumeapi.auth.repository.AdminUserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOidcUserServiceTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    @Mock
    private OidcUserRequest userRequest;

    @Test
    void enabledAdminGetsRoleAdminAndOriginalAuthorities() {
        AdminOidcUserService service = serviceWithUser("Admin@Example.com", true,
                List.of(new SimpleGrantedAuthority("OIDC_USER")));
        when(adminUserRepository.findByEmailIgnoreCase("Admin@Example.com"))
                .thenReturn(Optional.of(enabledAdmin("Admin@Example.com")));

        OidcUser result = service.loadUser(userRequest);

        assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority("OIDC_USER")));
        assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void emailLookupMatchesDifferentCase() {
        AdminOidcUserService service = serviceWithUser("Admin@Example.com", true, List.of());
        when(adminUserRepository.findByEmailIgnoreCase("Admin@Example.com"))
                .thenReturn(Optional.of(enabledAdmin("admin@example.com")));

        OidcUser result = service.loadUser(userRequest);

        assertEquals("google-subject", result.getSubject());
    }

    @Test
    void unknownEmailIsRejected() {
        AdminOidcUserService service = serviceWithUser("unknown@example.com", true, List.of());
        when(adminUserRepository.findByEmailIgnoreCase("unknown@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(OAuth2AuthenticationException.class, () -> service.loadUser(userRequest));
    }

    @Test
    void disabledAdminIsRejected() {
        AdminOidcUserService service = serviceWithUser("disabled@example.com", true, List.of());
        when(adminUserRepository.findByEmailIgnoreCase("disabled@example.com"))
                .thenReturn(Optional.of(disabledAdmin("disabled@example.com")));

        assertThrows(OAuth2AuthenticationException.class, () -> service.loadUser(userRequest));
    }

    @Test
    void unverifiedEmailIsRejected() {
        AdminOidcUserService service = serviceWithUser("admin@example.com", false, List.of());

        assertThrows(OAuth2AuthenticationException.class, () -> service.loadUser(userRequest));
    }

    @Test
    void nullEmailVerifiedIsRejected() {
        AdminOidcUserService service = serviceWithUser("admin@example.com", null, List.of());

        assertThrows(OAuth2AuthenticationException.class, () -> service.loadUser(userRequest));
    }

    @Test
    void nullEmailIsRejected() {
        AdminOidcUserService service = serviceWithUser(null, true, List.of());

        assertThrows(OAuth2AuthenticationException.class, () -> service.loadUser(userRequest));
    }

    private AdminOidcUserService serviceWithUser(
            String email, Boolean verified, List<SimpleGrantedAuthority> authorities) {
        OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getEmail()).thenReturn(email);
        when(oidcUser.getEmailVerified()).thenReturn(verified);
        lenient().doReturn(authorities).when(oidcUser).getAuthorities();
        lenient().when(oidcUser.getIdToken()).thenReturn(idToken());
        lenient().when(oidcUser.getUserInfo()).thenReturn(new OidcUserInfo(Map.of("sub", "google-subject")));

        AdminOidcUserService service = spy(new AdminOidcUserService(adminUserRepository));
        doReturn(oidcUser).when(service).loadOidcUser(any(OidcUserRequest.class));
        return service;
    }

    private OidcIdToken idToken() {
        Instant issuedAt = Instant.now();
        return new OidcIdToken("id-token", issuedAt, issuedAt.plusSeconds(300),
                Map.of("sub", "google-subject"));
    }

    private AdminUser enabledAdmin(String email) {
        AdminUser admin = new AdminUser("admin", "hash");
        admin.setEmail(email);
        return admin;
    }

    private AdminUser disabledAdmin(String email) {
        AdminUser admin = enabledAdmin(email);
        admin.setEnabled(false);
        return admin;
    }
}
