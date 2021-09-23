package net.apmoller.crb.ohp.microservices.handler;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import net.apmoller.crb.ohp.microservices.exception.InvalidEnumException;
import net.apmoller.crb.ohp.microservices.model.ApiError;
import net.apmoller.crb.ohp.microservices.model.ApiSubError;
import net.apmoller.crb.ohp.microservices.model.ApiValidationError;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebInputException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@ConditionalOnProperty(value = "spring.application.reactive-handlers.enabled", havingValue = "true")
public class ServerWebInputExceptionHandler extends AnnotatedExceptionHandler {

    private static final String DEFAULT_ERROR_FIELD = "";
    private static final String API_VALIDATION_ERROR_MESSAGE = "Validation errors";
    private static final String INVALID_FORMAT_SUBERROR_MESSAGE = "Invalid format";
    private static final String INVALID_VALUE_SUBERROR_MESSAGE = "Invalid value";

    @Override
    @ExceptionHandler({ServerWebInputException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiError> handleServerWebInputException(ServerWebInputException serverWebInputException,
                                                                  ServerHttpRequest serverHttpRequest) {
        var apiError = createApiError(serverWebInputException, serverHttpRequest);
        if (serverWebInputException.getCause() instanceof DecodingException) {
            var decodingException = (DecodingException) serverWebInputException.getCause();
            var apiValidationError = getApiValidationError(decodingException);
            Optional.ofNullable(apiValidationError)
                    .ifPresent(validationError -> updateApiError(apiError, validationError));
        }
        logError(apiError.getSubErrors(), apiError);
        return buildResponseEntity(apiError);
    }

    private ApiError createApiError(ServerWebInputException serverWebInputException, ServerHttpRequest serverHttpRequest) {
        return new ApiError(serverHttpRequest.getMethod(), serverHttpRequest.getPath().value(),
                HttpStatus.BAD_REQUEST, API_VALIDATION_ERROR_MESSAGE, serverWebInputException);
    }

    private ApiValidationError getApiValidationError(DecodingException decodingException) {
        if ((decodingException.getCause() instanceof InvalidFormatException)) {
            return buildApiValidationErrorForInvalidFormatException(decodingException);
        } else if (decodingException.getCause() instanceof ValueInstantiationException) {
            return buildApiValidationErrorForValueInstantiationException(decodingException);
        }
        return null;
    }

    private ApiValidationError buildApiValidationErrorForInvalidFormatException(DecodingException decodingException) {
        var invalidFormatException = (InvalidFormatException) decodingException.getCause();
        var path = createPathBuilder(invalidFormatException.getPath());
        return buildApiValidationError(path, invalidFormatException.getValue().toString(), INVALID_FORMAT_SUBERROR_MESSAGE);
    }

    private ApiValidationError buildApiValidationErrorForValueInstantiationException(DecodingException decodingException) {
        var valueInstantiationException = (ValueInstantiationException) decodingException.getCause();
        var path = createPathBuilder(valueInstantiationException.getPath());
        var rejectedValue = getRejectedValueForValueInstantiationException(valueInstantiationException);
        return buildApiValidationError(path, rejectedValue, INVALID_VALUE_SUBERROR_MESSAGE);
    }

    private String createPathBuilder(List<JsonMappingException.Reference> references) {
        return references.stream()
                .map(JsonMappingException.Reference::getFieldName)
                .collect(Collectors.joining("."));
    }

    private String getRejectedValueForValueInstantiationException(ValueInstantiationException e) {
        if (e.getCause() instanceof InvalidEnumException) {
            return Optional.ofNullable(((InvalidEnumException) e.getCause()).getApiValidationError())
                    .map(ApiValidationError::getRejectedValue)
                    .map(Object::toString)
                    .orElse(null);
        }
        return null;
    }

    private ApiValidationError buildApiValidationError(String path, String rejectedValue, String message) {
        return new ApiValidationError(path.length() > 0 ? path : DEFAULT_ERROR_FIELD, rejectedValue, message);
    }

    private void updateApiError(ApiError apiError, ApiValidationError validationError) {
        apiError.setDebugMessage(null);
        apiError.setSubErrors(List.of(validationError));
    }

    public static void logError(List<ApiSubError> subErrors, ApiError apiError) {
        log.error("Returning error response with status {} and subErrors {}", apiError.getStatus(), subErrors);
    }

    public static ResponseEntity<ApiError> buildResponseEntity(ApiError apiError) {
        return ResponseEntity.status(apiError.getStatus()).contentType(MediaType.APPLICATION_JSON).body(apiError);
    }
}
