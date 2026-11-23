package com.shaahil.resumeapi.ai;

import com.shaahil.resumeapi.PostgresTestContainerConfig;
import com.shaahil.resumeapi.ai.chat.AnswerStreamer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

@SpringBootTest
@ActiveProfiles("test")
@Import(ChatSecurityIT.TestChatConfiguration.class)
class ChatSecurityIT extends PostgresTestContainerConfig {

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void postWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"hi\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postWithTokenWithoutChatScopeReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/ai/chat")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_contact:submit")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"hi\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void postWithChatScopeStreamsEventStream() throws Exception {
        var result = mockMvc.perform(post("/api/ai/chat")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_ai:chat")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"hi\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }

    @Test
    void bffClientHasContactAndChatScopes() {
        var bff = registeredClientRepository.findByClientId("bff");

        assertThat(bff).isNotNull();
        assertThat(bff.getScopes()).containsExactlyInAnyOrder("contact:submit", "ai:chat");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestChatConfiguration {

        @Bean
        @Primary
        AnswerStreamer answerStreamer() {
            return (systemMessage, userMessage) -> Flux.just("test answer");
        }
    }
}
