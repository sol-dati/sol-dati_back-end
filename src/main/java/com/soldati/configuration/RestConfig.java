package com.soldati.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

// RestTemplate unico, condiviso dai client verso SonarQube e GitLab
@Configuration
public class RestConfig {

	@Bean
	RestTemplate restTemplate() {
		return new RestTemplate();
	}
}
