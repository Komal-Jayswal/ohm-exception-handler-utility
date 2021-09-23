package net.apmoller.crb.ohp.microservices.handler;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import net.apmoller.crb.ohp.microservices.exception.*;
import net.apmoller.crb.ohp.microservices.model.ApiError;
import net.apmoller.crb.ohp.microservices.model.ApiSubError;
import net.apmoller.crb.ohp.microservices.model.ApiValidationError;
import net.apmoller.crb.ohp.microservices.util.ExceptionUtils;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import javax.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReactiveErrorHandler {

    /**
     * This method is created to handle BAD_REQUEST : 400 exception
     *
     * @return ServerResponse with 400 status
     */
    public HandlerFilterFunction<ServerResponse, ServerResponse> badRequest() {
        return (request, next) -> next.handle( request )
                .onErrorResume( BadRequestException.class, e -> {
                    ApiError apiError = new ApiError( request.method(), request.path(),
                            HttpStatus.BAD_REQUEST, ExceptionUtils.BAD_REQUEST_DESCRIPTION, e );

                    ExceptionUtils.logError( apiError );

                    return ServerResponse
                            .badRequest()
                            .body(BodyInserters.fromValue(apiError));
                } );
    }

    /**
     * This method is created to handle Bind exception i.e.BAD_REQUEST : 400 exception
     *
     * @return ServerResponse with 400 status
     */
    public HandlerFilterFunction<ServerResponse, ServerResponse> bindException() {
        return (request, next) -> next.handle( request )
                .onErrorResume( BindException.class, e -> {
                    ApiError apiError = new ApiError( request.method(), request.path(),
                            HttpStatus.BAD_REQUEST, ExceptionUtils.VALIDATION_ERROR_DESCRIPTION, e );

                    List<ApiSubError> validationSubErrors = new ArrayList<>();

                    for (FieldError fieldError : e.getFieldErrors()) {
                        ApiValidationError validationError = new ApiValidationError( fieldError.getField(), fieldError.getRejectedValue(), fieldError.getDefaultMessage() );
                        validationSubErrors.add( validationError );
                    }
                    apiError.setSubErrors( validationSubErrors );
                    ExceptionUtils.logError( apiError );

                    return ServerResponse
                            .badRequest()
                            .body(BodyInserters.fromValue(apiError));
                } );
    }

    /**
     * This method is created to handle {@link ConstraintViolationException}  i.e.BAD_REQUEST : 400 exception
     *
     * @return ServerResponse with 400 status
     */
    public HandlerFilterFunction<ServerResponse, ServerResponse> constraintViolationException() {
        return (request, next) -> next.handle( request ).onErrorResume( ConstraintViolationException.class, e -> {
            ApiError apiError = new ApiError( request.method(), request.path(), HttpStatus.BAD_REQUEST, ExceptionUtils.VALIDATION_ERROR_DESCRIPTION, e );

            List<ApiSubError> validationSubErrors = new ArrayList<>();

            ExceptionUtils.constructValidationErrorObject( e, validationSubErrors );
            apiError.setSubErrors( validationSubErrors );
            ExceptionUtils.logError( apiError );

            return ServerResponse.badRequest().body(BodyInserters.fromValue(apiError));
        } );
    }


    /**
     * This method is created to handle NOT_FOUND : 404 exception
     *
     * @return ServerResponse with 404 status
     */
    public HandlerFilterFunction<ServerResponse, ServerResponse> notFound() {
        return (request, next) -> next.handle( request )
                .onErrorResume( DataNotFoundException.class, e -> {
                    ApiError apiError = new ApiError( request.method(), request.path(),
                            HttpStatus.NOT_FOUND, ExceptionUtils.DATA_NOT_FOUND_DESCRIPTION, e );

                    ExceptionUtils.logError( apiError );

                    return ServerResponse
                            .status(HttpStatus.NOT_FOUND)
                            .body(BodyInserters.fromValue(apiError));
                } );
    }

    /**
     * This method is created to handle incorrect uri errors.
     *
     * @return ServerResponse with response status
     */
    public HandlerFilterFunction<ServerResponse, ServerResponse> responseStatus() {
        return (request, next) -> next.handle( request )
                .onErrorResume( ResponseStatusException.class, e -> {
                    ApiError apiError = new ApiError( request.method(), request.path(),
                            e.getStatus(), e.getLocalizedMessage(), e );
                    ExceptionUtils.logError( apiError );
                    return ServerResponse
                            .status(e.getStatus())
                            .body(BodyInserters.fromValue(apiError));
                } );
    }

    /**
     * This method is created to handle input exception like DecodingException, empty body in request body for reactive api.
     *
     * @return ServerResponse with response status
     */
    public HandlerFilterFunction<ServerResponse, ServerResponse> serverWebInputException() {
        return (request, next) -> next.handle( request )
                .onErrorResume( ServerWebInputException.class, e -> {
                    ApiError apiError = new ApiError( request.method(), request.path(), HttpStatus.BAD_REQUEST, "Bad Request", e );
                    // If the root cause is due to a invalid format exception, add a validation sub error with the field name for which it failed
                    if (e.getCause() instanceof DecodingException) {
                        DecodingException decodingException = (DecodingException) e.getCause();
                        if (decodingException.getCause() instanceof InvalidFormatException) {
                            InvalidFormatException invalidFormatException = (InvalidFormatException) decodingException.getCause();
                            List<JsonMappingException.Reference> references = invalidFormatException.getPath();
                            StringBuilder path = new StringBuilder();
                            for (JsonMappingException.Reference reference : references) {
                                path.append( reference.getFieldName() ).append( "." );
                            }

                            ApiValidationError validationError = new ApiValidationError( path.length() > 0 ? path.substring( 0, path.length() - 1 ) : "", invalidFormatException.getValue(), "Invalid format" );
                            apiError.setSubErrors( Collections.singletonList( validationError ) );
                        }
                    }
                    ExceptionUtils.logError( apiError );
                    apiError.setDebugMessage( null );


                    return ServerResponse
                            .status(apiError.getStatus())
                            .body(BodyInserters.fromValue(apiError));
                } );
    }

    /**
     * This method is created to handle UNAUTHORIZED : 401 exception
     *
     * @return ServerResponse with 401 status
     */
    public HandlerFilterFunction<ServerResponse, ServerResponse> unauthorized() {
        return (request, next) -> next.handle( request )
                .onErrorResume( UnauthorizedException.class, e -> {
                    ApiError apiError = new ApiError( request.method(), request.path(),
                            HttpStatus.UNAUTHORIZED, ExceptionUtils.UNAUTHORIZED_DESCRIPTION, e );

                    ExceptionUtils.logError( apiError );

                    return ServerResponse
                            .status(HttpStatus.UNAUTHORIZED)
                            .body(BodyInserters.fromValue(apiError));
                } );
    }

    /**
     * This method is created to handle FORBIDDEN : 403 exception
     *
     * @return ServerResponse with 403 status
     */
    public HandlerFilterFunction<ServerResponse, ServerResponse> forbidden() {
        return (request, next) -> next.handle( request )
                .onErrorResume( ForbiddenException.class, e -> {
                    ApiError apiError = new ApiError( request.method(), request.path(),
                            HttpStatus.FORBIDDEN, ExceptionUtils.FORBIDDEN_DESCRIPTION, e );

                    ExceptionUtils.logError( apiError );

                    return ServerResponse
                            .status(HttpStatus.FORBIDDEN)
                            .body(BodyInserters.fromValue(apiError));
                } );
    }

    /**
     * This method is created to handle INTERNAL_SERVER_ERROR : 500 exception
     *
     * @return ServerResponse with 500 status
     */
    public HandlerFilterFunction<ServerResponse, ServerResponse> catchAll() {
        return (request, next) -> next.handle( request )
                .onErrorResume( (Exception.class), e -> {
                    ApiError apiError = new ApiError( request.method(), request.path(),
                            HttpStatus.INTERNAL_SERVER_ERROR, ExceptionUtils.INTERNAL_SERVER_ERROR_DESCRIPTION, e );
                    ExceptionUtils.logError( apiError );

                    // Details of the error have been logged, however, as we are potentially dealing with an exception thrown by a
                    // third party library for which we have no control over the exception message, to prevent leaking system
                    // details to the client suppress the debug message containing this information.
                    apiError.setDebugMessage( null );

                    return ServerResponse
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(BodyInserters.fromValue(apiError));
                } );
    }

    /**
     * This method is created to handle and log webclient errors : depending on the error it might propagate it or return
     * 500 exception
     *
     * @return ServerResponse with the corresponding status
     */
    public HandlerFilterFunction<ServerResponse, ServerResponse> webClientErrors() {
        return (request, next) -> next.handle(request)
                .onErrorResume((WebClientResponseException.BadRequest.class), webClientException ->
                        wrapWebClientException(webClientException, request, webClientException.getStatusCode(), webClientException.getLocalizedMessage()))
                .onErrorResume((WebClientResponseException.Forbidden.class), webClientException ->
                        wrapWebClientException(webClientException, request, webClientException.getStatusCode(), webClientException.getLocalizedMessage()))
                .onErrorResume((WebClientResponseException.Unauthorized.class), webClientException ->
                        wrapWebClientException(webClientException, request, webClientException.getStatusCode(), webClientException.getLocalizedMessage()))
                .onErrorResume((WebClientResponseException.NotFound.class), webClientException ->
                        wrapWebClientException(webClientException, request, webClientException.getStatusCode(), webClientException.getLocalizedMessage()))
                .onErrorResume((WebClientResponseException.class), webClientException ->
                        wrapWebClientException(webClientException, request, HttpStatus.INTERNAL_SERVER_ERROR, ExceptionUtils.INTERNAL_SERVER_ERROR_DESCRIPTION));
    }

    /**
     * Utility method that can be used by consuming projects to validates a parameter based on a regular expression.
     * Will throw an exception if it is incorrect.
     *
     * @param parameterValueToValidate the value of the parameter we are validating.
     * @param parameterName            the name of the parameter.
     * @param parameterRegex           the regular expression that this parameter needs to pass.
     * @param mandatory                boolean representing if the parameter is mandatory on this query.
     * @throws BadRequestException Bad request ecxception
     */
    public String validateQueryParameter(String parameterValueToValidate, String parameterName,
                                         String parameterRegex, boolean mandatory) {
        if (mandatory && (parameterValueToValidate == null || parameterValueToValidate.trim().isEmpty())) {
            throw new BadRequestException("The parameter " + parameterName + " is a mandatory parameter.");
        }
        if (parameterValueToValidate != null && !parameterValueToValidate.matches(parameterRegex)) {
            throw new BadRequestException("The format of the value specified for field " + parameterName +
                    " is invalid, format must match " + parameterRegex);
        }
        return parameterValueToValidate;
    }

    /**
     * For all WebClientExceptions creates an InternalServerException and throws it
     *
     * @param webClientException input exception
     * @param request            Http Server Request
     * @param status             the returned HTTP Status
     * @param localizedMessage   the response localizedMessage
     * @return the corresponding InternalServerError exception
     */
    private Mono<ServerResponse> wrapWebClientException(WebClientResponseException webClientException,
                                                        ServerRequest request, HttpStatus status, String localizedMessage) {
        ApiError apiError = new ApiError(request.method(), request.path(),
                status, localizedMessage, webClientException);
        apiError.setDebugMessage(webClientException.getLocalizedMessage() + ", response body is " +
                webClientException.getResponseBodyAsString());
        ExceptionUtils.logError(apiError);

        // Details of the error have been logged, however, as we are potentially dealing with an exception thrown by a
        // third party library for which we have no control over the exception message, to prevent leaking system
        // details to the client suppress the debug message containing this information.
        apiError.setDebugMessage(null);

        return ServerResponse
                .status(status)
                .body(BodyInserters.fromValue(apiError));
    }
}
