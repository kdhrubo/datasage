package io.ninetiger.datasage.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAIConfiguration {

    @Bean
    public ChatClient openAIChatClient(OpenAiChatModel chatModel) {
        return ChatClient.create(chatModel);
    }

}
