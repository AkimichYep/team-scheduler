package com.scheduler;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AiSchedulerService {

    private final ChatClient chatClient;

    public AiSchedulerService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String suggestSchedule(String teamInfo) {
        return chatClient.prompt()
                .user("Given this team info: " + teamInfo + ", suggest an optimal schedule.")
                .call()
                .content();
    }
}