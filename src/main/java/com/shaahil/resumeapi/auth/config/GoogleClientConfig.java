package com.shaahil.resumeapi.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

/**
 * Registers Google as an OAuth2 client so this Authorization Server can act
 * as a client of Google's own authorization server — the Google half of the
 * federated-login path. spring-boot-starter-oauth2-client uses this
 * registration to run the whole Authorization Code flow against Google: the
 * authorize/token endpoints, scopes, redirect URI, everything needed to send
 * the user to Google and get an ID token back.
 */
@Configuration
public class GoogleClientConfig {
/**

 ClientRegistrationRepository = the list of outside login providers we trust (here: only Google).
 What it holds: for each provider, its name ("google"), our client-id and client-secret,
 and Google's URLs (login page, token URL, JWKS URL). ClientRegistrations.fromIssuerLocation(
 "https://accounts.google.com") reads these URLs automatically from Google, so we don't type them.
 Who uses it: .oauth2Login(Customizer.withDefaults()) in SecurityConfig. When that switch is on,
 Spring looks in this list to know where to send the user (Google) and how to check what comes back.
 Flow in simple words:
 User clicks "Login with Google" -> /oauth2/authorization/google.
 Spring finds "google" in this repository and redirects the user to Google.
 Google signs the user in and sends them back with a code.
 Spring swaps the code for Google's ID token and checks it using Google's own JWKS.
 User is now logged in on our server. Google's token is not used again.
 Not related to our own JWT: that is made later by our Authorization Server (Order 1)
 with our own private key and our own kid.
 **/
    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${spring.security.oauth2.client.registration.google.client-id}") String clientId,
            @Value("${spring.security.oauth2.client.registration.google.client-secret}") String clientSecret) {

        ClientRegistration googleRegistration = ClientRegistrations
                .fromIssuerLocation("https://accounts.google.com")
                .registrationId("google")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .scope("openid", "profile", "email")
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .build();

        return new InMemoryClientRegistrationRepository(googleRegistration);
    }
}
