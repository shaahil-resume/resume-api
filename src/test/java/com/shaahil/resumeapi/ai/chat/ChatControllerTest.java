package com.shaahil.resumeapi.ai.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ChatController(null))
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
}
