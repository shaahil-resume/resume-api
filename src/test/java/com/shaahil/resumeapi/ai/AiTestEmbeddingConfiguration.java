package com.shaahil.resumeapi.ai;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class AiTestEmbeddingConfiguration {

    @Bean
    @Primary
    DeterministicEmbeddingModel embeddingModel() {
        return new DeterministicEmbeddingModel();
    }
}
