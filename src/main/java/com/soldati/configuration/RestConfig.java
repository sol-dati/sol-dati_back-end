package com.soldati.configuration;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

// RestTemplate unico, condiviso dai client verso SonarQube e GitLab, i timeout evitano che una chiamata senza risposta blocchi l'analisi
@Configuration
public class RestConfig {

    @Bean
    RestTemplate restTemplate(@Value("${http.connect.timeout.secondi:10}") long connectTimeout,
                              @Value("${http.read.timeout.secondi:120}") long readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(connectTimeout));
        factory.setReadTimeout(Duration.ofSeconds(readTimeout));
        return new RestTemplate(factory);
    }
}