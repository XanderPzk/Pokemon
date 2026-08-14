package com.alex.pokedex.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(PokeApiConfig.PokeApiProperties.class)
public class PokeApiConfig {

    @ConfigurationProperties(prefix = "pokeapi")
    public record PokeApiProperties(
            String baseUrl, Duration connectTimeout, Duration readTimeout) {}

    @Bean
    RestClient pokeApiRestClient(RestClient.Builder builder, PokeApiProperties properties) {
        return builder.baseUrl(properties.baseUrl())
                .requestFactory(
                        ClientHttpRequestFactories.get(
                                ClientHttpRequestFactorySettings.DEFAULTS
                                        .withConnectTimeout(properties.connectTimeout())
                                        .withReadTimeout(properties.readTimeout())))
                .build();
    }
}
