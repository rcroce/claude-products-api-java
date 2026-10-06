package com.claudeproducts.api.shared;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
class WebConfig implements WebMvcConfigurer {

	private final CorsProperties cors;

	WebConfig(CorsProperties cors) {
		this.cors = cors;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
			.allowedOrigins(cors.allowedOrigins().toArray(String[]::new))
			.allowedMethods("GET", "POST", "PUT", "DELETE")
			.allowedHeaders(HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT, HttpHeaders.AUTHORIZATION)
			.exposedHeaders(HttpHeaders.LOCATION);
	}

}
