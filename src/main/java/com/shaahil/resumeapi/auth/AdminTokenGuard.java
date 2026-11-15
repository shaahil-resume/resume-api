package com.shaahil.resumeapi.auth;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

@Component
public class AdminTokenGuard {

    private final JwtDecoder jwtDecoder;

    public AdminTokenGuard(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    public boolean decodeAndValidate(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        final Jwt jwt;
        try {
            jwt = jwtDecoder.decode(token);
        } catch (JwtException ex) {
            return false;
        }

        var roles = jwt.getClaimAsStringList("roles");
        if (roles == null || !roles.contains(AuthConstants.ROLE_ADMIN)) {
            return false;
        }

        return jwt.getAudience() != null && jwt.getAudience().contains("resume-api");
    }
}
