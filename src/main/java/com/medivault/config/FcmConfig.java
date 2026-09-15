package com.medivault.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FcmConfig {

	@Bean
	FcmSettings fcmSettings(@Value("${medivault.firebase.credentials-path:}") String credentialsPath) {
		return new FcmSettings(credentialsPath);
	}

	public record FcmSettings(String credentialsPath) {
		public boolean configured() {
			return credentialsPath != null && !credentialsPath.isBlank();
		}
	}
}
