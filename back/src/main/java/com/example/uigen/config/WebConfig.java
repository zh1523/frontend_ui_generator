package com.example.uigen.config;

import com.example.uigen.common.ApiRequestLoggingInterceptor;
import com.example.uigen.common.AuthInterceptor;
import java.util.Arrays;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties appProperties;
    private final ApiRequestLoggingInterceptor apiRequestLoggingInterceptor;
    private final AuthInterceptor authInterceptor;

    public WebConfig(AppProperties appProperties,
                     ApiRequestLoggingInterceptor apiRequestLoggingInterceptor,
                     AuthInterceptor authInterceptor) {
        this.appProperties = appProperties;
        this.apiRequestLoggingInterceptor = apiRequestLoggingInterceptor;
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = Arrays.stream(appProperties.cors().allowedOrigins().split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toArray(String[]::new);
        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Content-Disposition");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**");
        registry.addInterceptor(apiRequestLoggingInterceptor)
                .addPathPatterns("/api/**");
    }
}
