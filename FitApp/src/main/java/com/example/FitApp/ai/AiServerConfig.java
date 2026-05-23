package com.example.FitApp.ai;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AiServerConfig {

    @Bean
    public RestClient aiRestClient() {
        return RestClient.builder().build();
    }
}
