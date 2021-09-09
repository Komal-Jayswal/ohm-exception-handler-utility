package net.apmoller.crb.ohp.microservices.config;

import lombok.AllArgsConstructor;
import net.apmoller.crb.ohp.microservices.handler.AnnotatedExceptionHandler;
import net.apmoller.crb.ohp.microservices.handler.ExceptionHandlers;
import net.apmoller.crb.ohp.microservices.handler.GlobalErrorWebExceptionHandler;
import net.apmoller.crb.ohp.microservices.model.ApiError;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.codec.support.DefaultServerCodecConfigurer;

/**
 * Configuration class for initializing beans.
 */
@Configuration
@AllArgsConstructor
public class SpringBootComponentsConfig {

    @Bean
    @ConditionalOnProperty(value = "spring.application.reactive-handlers.enabled", havingValue = "true")
    public GlobalErrorWebExceptionHandler globalErrorHandler(ApiError apiError, ApplicationContext applicationContext) {
        return new GlobalErrorWebExceptionHandler(apiError, applicationContext, new DefaultServerCodecConfigurer());
    }

    @Bean
    @ConditionalOnProperty(value = "spring.application.annotated-exception-handlers.enabled", havingValue = "true")
    public AnnotatedExceptionHandler exceptionHandlers() {
        return new AnnotatedExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean(ApiError.class)
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public ApiError apiError() {
        return new ApiError();
    }

    @Bean
    @ConditionalOnProperty(value = "spring.application.non-reactive-handlers.enabled", havingValue = "true")
    public ExceptionHandlers nonReactiveExceptionHandlers() {
        return new ExceptionHandlers();
    }
}
