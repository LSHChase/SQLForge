package com.sqlforge.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebCorsConfig implements WebMvcConfigurer {

    @Value("${sqlforge.cors.allowed-origin-patterns:http://localhost:* ,http://127.0.0.1:*}")
    private String allowedOriginPatterns;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOriginPatterns(parsePatterns())
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*");
    }

    private String[] parsePatterns() {
        String[] rawPatterns = allowedOriginPatterns.split(",");
        java.util.List<String> patterns = new java.util.ArrayList<String>();

        for (String rawPattern : rawPatterns) {
            String value = rawPattern.trim();
            if (!value.isEmpty()) {
                patterns.add(value);
            }
        }

        return patterns.toArray(new String[patterns.size()]);
    }
}
