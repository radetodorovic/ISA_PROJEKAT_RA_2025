package com.isa.backend.monitoring;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MonitoringWebConfig implements WebMvcConfigurer {
    private final ActiveUserTrackingInterceptor activeUserTrackingInterceptor;

    public MonitoringWebConfig(ActiveUserTrackingInterceptor activeUserTrackingInterceptor) {
        this.activeUserTrackingInterceptor = activeUserTrackingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(activeUserTrackingInterceptor);
    }
}
