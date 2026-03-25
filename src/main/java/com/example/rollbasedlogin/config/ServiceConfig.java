package com.example.rollbasedlogin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.example.rollbasedlogin.service.DocumentParsingService;
import com.example.rollbasedlogin.service.HuggingFaceAIService;

/**
 * Configuration class to register services as Spring beans
 */
@Configuration
public class ServiceConfig {
    
    @Bean
    public DocumentParsingService documentParsingService(HuggingFaceAIService huggingFaceAIService) {
        return new DocumentParsingService(huggingFaceAIService);
    }
}
