package com.shaahil.resumeapi.ai.ingest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class IngestionConfiguration {

    @Bean
    KnowledgeFileParser knowledgeFileParser() {
        return new KnowledgeFileParser();
    }
}
