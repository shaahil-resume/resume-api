package com.shaahil.resumeapi.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTokenGuardTest {

    @Mock
    private JwtDecoder jwtDecoder;

    @Test
    void validAdminTokenReturnsTrue() {
        when(jwtDecoder.decode("valid-token")).thenReturn(jwt(List.of("ROLE_ADMIN"), List.of("resume-api")));

        assertTrue(guard().decodeAndValidate("valid-token"));
    }

    @Test
    void tokenWithRolesMissingReturnsFalse() {
        when(jwtDecoder.decode("pitcher-token")).thenReturn(jwt(null, List.of("resume-api")));

        assertFalse(guard().decodeAndValidate("pitcher-token"));
    }

    @Test
    void tokenWithNonAdminRoleReturnsFalse() {
        when(jwtDecoder.decode("user-token")).thenReturn(jwt(List.of("ROLE_USER"), List.of("resume-api")));

        assertFalse(guard().decodeAndValidate("user-token"));
    }

    @Test
    void tokenWithWrongAudienceReturnsFalse() {
        when(jwtDecoder.decode("wrong-audience-token"))
                .thenReturn(jwt(List.of("ROLE_ADMIN"), List.of("other-api")));

        assertFalse(guard().decodeAndValidate("wrong-audience-token"));
    }

    @Test
    void decodeThrowingJwtExceptionReturnsFalse() {
        when(jwtDecoder.decode("invalid-token")).thenThrow(new JwtException("invalid token"));

        assertFalse(guard().decodeAndValidate("invalid-token"));
    }

    @Test
    void nullTokenReturnsFalse() {
        assertFalse(guard().decodeAndValidate(null));
    }

    private AdminTokenGuard guard() {
        return new AdminTokenGuard(jwtDecoder);
    }

    private Jwt jwt(List<String> roles, List<String> audience) {
        var builder = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .audience(audience);
        if (roles != null) {
            builder.claim("roles", roles);
        }
        return builder.build();
    }
}
