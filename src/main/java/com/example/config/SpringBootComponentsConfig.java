package com.example.config;

import lombok.AllArgsConstructor;
import com.example.handler.ExceptionHandlers;
import com.example.model.ApiHTTPErrorResponse;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * Configuration class for initializing beans.
 */
@Configuration
@AllArgsConstructor
public class SpringBootComponentsConfig {

    @Bean
    @ConditionalOnMissingBean(ApiHTTPErrorResponse.class)
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public ApiHTTPErrorResponse apiError() {
        return new ApiHTTPErrorResponse();
    }

    @Bean
    @ConditionalOnProperty(value = "spring.application.non-reactive-handlers.enabled", havingValue = "true")
    public ExceptionHandlers nonReactiveExceptionHandlers() {
        return new ExceptionHandlers();
    }
}
