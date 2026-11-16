package com.shaahil.resumeapi.auth;

import com.shaahil.resumeapi.auth.entity.AdminUser;
import com.shaahil.resumeapi.auth.repository.AdminUserRepository;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class AdminOidcUserService extends OidcUserService {

    private final AdminUserRepository adminUserRepository;

    public AdminOidcUserService(AdminUserRepository adminUserRepository) {
        this.adminUserRepository = adminUserRepository;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = loadOidcUser(userRequest);
        String email = oidcUser.getEmail();
        Boolean verified = oidcUser.getEmailVerified();

        if (email == null || email.isBlank() || !Boolean.TRUE.equals(verified)) {
            throw adminNotAllowed();
        }

        AdminUser admin = adminUserRepository.findByEmailIgnoreCase(email)
                .filter(AdminUser::isEnabled)
                .orElseThrow(this::adminNotAllowed);

        Set<GrantedAuthority> authorities = new LinkedHashSet<>(adminAuthorities(oidcUser));
        authorities.add(new SimpleGrantedAuthority(AuthConstants.ROLE_ADMIN));

        return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
    }

    protected OidcUser loadOidcUser(OidcUserRequest userRequest) {
        return super.loadUser(userRequest);
    }

    private Set<GrantedAuthority> adminAuthorities(OidcUser oidcUser) {
        return new LinkedHashSet<>(oidcUser.getAuthorities());
    }

    private OAuth2AuthenticationException adminNotAllowed() {
        return new OAuth2AuthenticationException(new OAuth2Error(AuthConstants.ADMIN_NOT_ALLOWED));
    }
}
