package com.shaahil.resumeapi.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Seeds the OAuth2 clients when the application starts. It checks the client id first so restarts
 * do not create duplicates. Secrets come from environment variables so they are not stored in code.
 */
@Component
@RequiredArgsConstructor
public class ClientSeeder implements ApplicationRunner {

    private final RegisteredClientRepository clients;
    private final PasswordEncoder passwordEncoder;

    @Value("${clients.postman.secret}")
    private String postmanSecret;

    @Value("${clients.bff-admin.secret}")
    private String bffAdminSecret;

    @Value("${clients.bff-admin.redirect-uri}")
    private String bffAdminRedirectUri;

    @Value("${clients.pitcher.secret}")
    private String pitcherSecret;

    @Value("${clients.ai-module.secret}")
    private String aiModuleSecret;

    @Value("${clients.bff.secret}")
    private String bffSecret;

    @Override
    public void run(ApplicationArguments args) {
        saveIfMissing(postman());
        saveIfMissing(bffAdmin());
        saveIfMissing(pitcher());
        saveIfMissing(aiModule());
        saveIfMissing(bff());
    }

    private void saveIfMissing(RegisteredClient client) {
        if (clients.findByClientId(client.getClientId()) == null) {
            clients.save(client);
        }
    }

    private RegisteredClient postman() {
        return RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("postman")
                .clientSecret(passwordEncoder.encode(postmanSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://oauth.pstmn.io/v1/callback")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .clientSettings(ClientSettings.builder().requireProofKey(true).build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(15))
                        .build())
                .build();
    }

    private RegisteredClient bffAdmin() {
        return RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("bff-admin")
                .clientSecret(passwordEncoder.encode(bffAdminSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(bffAdminRedirectUri)
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .clientSettings(ClientSettings.builder().requireProofKey(true).build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(15))
                        .build())
                .build();
    }

    private RegisteredClient pitcher() {
        return RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("pitcher")
                .clientName("pitcher")
                .clientSecret(passwordEncoder.encode(pitcherSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("resume:read")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(5))
                        .build())
                .build();
    }

    private RegisteredClient aiModule() {
        return RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("ai-module")
                .clientName("ai-module")
                .clientSecret(passwordEncoder.encode(aiModuleSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("profile:export")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(5))
                        .build())
                .build();
    }

    private RegisteredClient bff() {
        return RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("bff")
                .clientName("bff")
                .clientSecret(passwordEncoder.encode(bffSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("contact:submit")
                .scope("ai:chat")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(5))
                        .build())
                .build();
    }
}
