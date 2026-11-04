package com.example.Oboe.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration

public class AppConfig implements WebMvcConfigurer {

    @Value("${app.file-storage.dir:uploads}")
    private String storageDir;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    // Phục vụ file đã upload (thay cho AWS S3) tại http://localhost:8080/uploads/**
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(storageDir).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}
