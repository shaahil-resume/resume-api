package com.shaahil.resumeapi.ai.limits;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Who the visitor is for rate limiting. Callers are the site's backend, which forwards the
 * visitor's address in X-Forwarded-For; the first entry is used. Without that header every
 * visitor would share the backend's own address and one rate bucket.
 */
public final class VisitorAddress {

    private static final int MAX_LENGTH = 64;

    private VisitorAddress() {
    }

    public static String of(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String address = forwarded == null || forwarded.isBlank()
                ? request.getRemoteAddr()
                : forwarded.split(",", 2)[0].trim();
        if (address == null || address.isBlank()) {
            return "unknown";
        }
        return address.length() > MAX_LENGTH ? address.substring(0, MAX_LENGTH) : address;
    }
}
