package net.apmoller.crb.ohp.microservices.handler;

import net.apmoller.crb.ohp.microservices.model.ApiError;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

@Order(-2)
public class GlobalErrorWebExceptionHandler extends DefaultErrorWebExceptionHandler {

    private static final String ERROR_ATTRIBUTE = DefaultErrorAttributes.class.getName() + ".ERROR";
    private static final net.apmoller.crb.ohp.microservices.handler.ReactiveErrorHandler error = new net.apmoller.crb.ohp.microservices.handler.ReactiveErrorHandler();

    public GlobalErrorWebExceptionHandler(ApiError apiError, ApplicationContext applicationContext, ServerCodecConfigurer serverCodecConfigurer) {
        super(apiError, new WebProperties.Resources(), new ErrorProperties(), applicationContext);
        super.setMessageWriters(serverCodecConfigurer.getWriters());
        super.setMessageReaders(serverCodecConfigurer.getReaders());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse)
                .filter(error.notFound())
                .filter(error.bindException())
                .filter(error.constraintViolationException())
                .filter(error.badRequest())
                .filter(error.unauthorized())
                .filter(error.forbidden())
                .filter(error.webClientErrors())
                .filter(error.serverWebInputException())
                .filter(error.responseStatus())
                .filter(error.catchAll());
    }


    /**
     * Returns the error thrown in web exchange as-is to be handled by router filter functions added above.
     *
     * @param request server request
     * @return server response
     */
    @Override
    protected Mono<ServerResponse> renderErrorResponse(final ServerRequest request) {
        return Mono.error((Throwable) request.attribute(ERROR_ATTRIBUTE).orElseThrow(() ->
                new IllegalStateException("Missing exception attribute in ServerWebExchange")
        ));
    }

}