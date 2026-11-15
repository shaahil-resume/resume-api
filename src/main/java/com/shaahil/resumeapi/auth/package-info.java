/**
 * Authentication and authorization module.
 * <p>
 * Owns the OAuth2 Authorization Server configuration, JWT issuance,
 * the registered client (frontend), and the admin user entity.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Auth"
)
package com.shaahil.resumeapi.auth;