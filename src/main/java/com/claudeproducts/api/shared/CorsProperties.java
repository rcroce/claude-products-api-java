package com.claudeproducts.api.shared;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Origens que podem chamar a API pelo navegador (app web e Expo web). */
@ConfigurationProperties("app.cors")
public record CorsProperties(List<String> allowedOrigins) {

	public CorsProperties {
		allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
	}

}
