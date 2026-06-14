package com.scheduler.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Spring AI ChatClient.
 * Sets up the ChatClient bean for use throughout the application.
 */
@Configuration
public class AiConfig {

    /**
     * Creates and configures the ChatClient bean.
     * The ChatClient is used for interacting with the GROQ LLM.
     *
     * @param builder The ChatClient builder provided by Spring AI
     * @return Configured ChatClient bean
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}

