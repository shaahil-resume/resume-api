package com.shaahil.resumeapi.ai.chat;

import com.shaahil.resumeapi.ai.limits.AiLimitsProperties;
import com.shaahil.resumeapi.ai.limits.VisitorRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Clock;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatControllerTest {

    private MockMvc mockMvc;
    private VisitorRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        rateLimiter = new VisitorRateLimiter(new AiLimitsProperties(1, 60, 500), Clock.systemUTC());
        mockMvc = MockMvcBuilders.standaloneSetup(new ChatController(null, rateLimiter))
                .setValidator(validator)
                .build();
    }

    @Test
    void blankQuestionIsRejected() throws Exception {
        mockMvc.perform(post("/api/ai/chat")
                        .contentType("application/json")
                        .content("{\"question\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void questionOverFiveHundredCharactersIsRejected() throws Exception {
        String question = "a".repeat(501);
        mockMvc.perform(post("/api/ai/chat")
                        .contentType("application/json")
                        .content("{\"question\":\"" + question + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void visitorOverTheRateLimitGetsTooManyRequestsWithRetryAfter() throws Exception {
        rateLimiter.check("203.0.113.7");

        mockMvc.perform(post("/api/ai/chat")
                        .header("X-Forwarded-For", "203.0.113.7, 10.0.0.2")
                        .contentType("application/json")
                        .content("{\"question\":\"What did he build?\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void invalidQuestionDoesNotUseUpTheVisitorsAllowance() throws Exception {
        mockMvc.perform(post("/api/ai/chat")
                        .header("X-Forwarded-For", "203.0.113.8")
                        .contentType("application/json")
                        .content("{\"question\":\"  \"}"))
                .andExpect(status().isBadRequest());

        org.assertj.core.api.Assertions.assertThat(rateLimiter.check("203.0.113.8").allowed()).isTrue();
    }
}
