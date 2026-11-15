package com.shaahil.resumeapi.auth.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.UUID;

/**
 * Central OAuth2 Authorization Server configuration for the auth module —
 * client registration, JWT signing keys, and the dedicated security filter
 * chain for /oauth2/* and OIDC endpoints.
 */
@Configuration
public class AuthorizationServerConfig {
    /**
     * Method 1: where OAuth2 clients (this app itself, the Next.js frontend,
     * anything that will call /oauth2/authorize or /oauth2/token) are
     * registered and looked up from. Postgres-backed via JdbcTemplate rather
     * than in-memory, so registrations survive a restart.
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    /**
     * Method 2: the RSA key pair this Authorization Server signs every JWT
     * with. Generated once at startup and wrapped as a JWKSource so
     * Spring's OAuth2AuthorizationServerConfiguration can expose the public
     * key at /oauth2/jwks for resource servers (like this one, via
     * JwtDecoder) to verify signatures against.
     *
     * JWKS in simple words: a web page (/oauth2/jwks) that lists our
     * public keys. Other services read it to check that a token really
     * came from us. Every token carries a "kid" (key id) label saying which
     * key signed it, so the checker picks the matching key from the list.
     *
     * Why RSA, and why two keys? Think of a stamp. The private key is the
     * stamp: only we have it, we use it to sign tokens, and it is never
     * shared. The public key is a sample anyone can use to check a stamp is
     * genuine, but not to make one. With one shared secret instead, every
     * service that checks tokens could also fake them.
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return (jwkSelector, context) -> jwkSelector.select(jwkSet);
    }

    private static KeyPair generateRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * JwtDecoder = the checker that reads a JWT and decides if it is real.
     *
     * In simple words: our server makes JWTs (signed with the private key from jwkSource).
     * Sometimes our own server also receives a JWT back (for example the /userinfo endpoint or
     * a refresh/logout call). This bean is how it checks that JWT is genuine:
     *   1. Read the "kid" from the JWT header.
     *   2. Find the matching public key in jwkSource.
     *   3. Verify the signature, and check the token is not expired.
     *
     * It uses the same jwkSource as the signer, so the keys always match.
     */
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }
    /**
     * AuthorizationServerSettings = the settings for our auth server, mainly its web addresses.
     *
     * In simple words: our server offers addresses like /oauth2/authorize (start login),
     * /oauth2/token (get a token) and /oauth2/jwks (public keys). This bean tells Spring what
     * those addresses are. We keep the standard paths, so we write
     * no extra code for them.
     *
     * We do set the issuer. It is the web address written into every token as iss. Other
     * services compare it with the token's iss, so it must be the same address in every request and
     * environment, not guessed from each incoming request. It comes from the property auth.issuer
     * in application.yml, for example http://localhost:8080 locally and the public https address in production.
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings(
            @Value("${auth.issuer}") String issuer) {
        return AuthorizationServerSettings.builder().issuer(issuer).build();
    }

    /**
     * The Order 1 chain = the rules for our auth server's own addresses only
     * (/oauth2/authorize, /oauth2/token, /oauth2/jwks, OIDC endpoints).
     *
     * In simple words:
     *  - @Order(1) means Spring tries this chain first.
     *  - oauth2AuthorizationServer(...) adds the standard auth server filters and limits this chain
     *    to the auth server addresses. Any other address (like /login) skips it and goes to
     *    the Order 2 chain in SecurityConfig.
     *  - .oidc(...) turns on OpenID Connect, so we also get ID tokens and /.well-known endpoints.
     *  - If a user is not logged in when they reach /oauth2/authorize, they are sent to /login
     *    (handled by Order 2). After login they come back here to continue.
     *  - .oauth2ResourceServer(...jwt()) lets this chain accept our own JWTs (uses jwtDecoder).
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        http.oauth2AuthorizationServer(Customizer.withDefaults());

        OAuth2AuthorizationServerConfigurer as =
                http.getConfigurer(OAuth2AuthorizationServerConfigurer.class);
        http.securityMatcher(as.getEndpointsMatcher());
        as.oidc(Customizer.withDefaults());

        http.exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"),
                        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
                .oauth2ResourceServer(rs -> rs.jwt(Customizer.withDefaults()));

        http.authorizeHttpRequests(a -> a.anyRequest().authenticated());

        return http.build();
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
        return context -> {
            // A. only customize access tokens; leave ID tokens untouched.
            if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                return;
            }

            // B. identify every access token as intended for the resume API.
            context.getClaims().audience(List.of("resume-api"));

            // C. add admin authorities only to authorization-code access tokens.
            if (AuthorizationGrantType.AUTHORIZATION_CODE.equals(context.getAuthorizationGrantType())) {
                List<String> roles = context.getPrincipal().getAuthorities().stream()
                        .filter(authority -> authority.getAuthority().startsWith("ROLE_"))
                        .map(authority -> authority.getAuthority())
                        .toList();
                context.getClaims().claim("roles", roles);
            }
        };
    }
}
