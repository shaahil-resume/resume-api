package com.shaahil.resumeapi.ai.limits;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class VisitorAddressTest {

    @Test
    void usesTheFirstForwardedAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", " 203.0.113.7 , 10.0.0.2");

        assertThat(VisitorAddress.of(request)).isEqualTo("203.0.113.7");
    }

    @Test
    void fallsBackToTheRemoteAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.9");

        assertThat(VisitorAddress.of(request)).isEqualTo("198.51.100.9");
    }

    @Test
    void veryLongValuesAreTruncated() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "a".repeat(500));

        assertThat(VisitorAddress.of(request)).hasSize(64);
    }
}
