package net.apmoller.crb.ohp.microservices.handler;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import lombok.extern.slf4j.Slf4j;
import net.apmoller.crb.ohp.microservices.exception.*;
import net.apmoller.crb.ohp.microservices.model.ApiError;
import net.apmoller.crb.ohp.microservices.model.ApiSubError;
import net.apmoller.crb.ohp.microservices.model.ApiValidationError;
import net.apmoller.crb.ohp.microservices.util.ExceptionUtils;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ServerWebInputException;

import javax.validation.ConstraintViolationException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static net.apmoller.crb.ohp.microservices.util.ExceptionUtils.*;

/**
 * Set of generic error handlers to be used by Spring Boot RestController classes.
 */
@RestControllerAdvice
@Slf4j
public class AnnotatedExceptionHandler {
    /**
     * Build the response entity to be returned.
     *
     * @param apiError API error object {@link ApiError}
     * @return responseEntity
     */
    private ResponseEntity<ApiError> buildResponseEntity(ApiError apiError) {

        return ResponseEntity
                .status(apiError.getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(apiError);
    }

    /**
     * Data not found exception handler.
     *
     * @param dataNotFoundException {@link DataNotFoundException}
     * @return responseEntity with status set to 404 and payload set to {@link ApiError}
     */
    @ExceptionHandler(value = DataNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ApiError> handleDataNotFoundException(DataNotFoundException dataNotFoundException,
                                                                ServerHttpRequest serverHttpRequest) {

        ApiError apiError = new ApiError(serverHttpRequest.getMethod(), serverHttpRequest.getPath().value(),
                HttpStatus.NOT_FOUND, DATA_NOT_FOUND_DESCRIPTION, dataNotFoundException);
        logError(apiError);
        return buildResponseEntity(apiError);
    }

    /**
     * DuplicateDataFoundException exception handler.
     *
     * @param duplicateDataFoundException {@link DuplicateDataFoundException}
     * @return responseEntity with status set to 409 and payload set to {@link ApiError}
     */
    @ExceptionHandler(value = DuplicateDataFoundException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ApiError> handleDuplicateDataFoundException(DuplicateDataFoundException duplicateDataFoundException, ServerHttpRequest serverHttpRequest) {
        ApiError apiError = new ApiError(serverHttpRequest.getMethod(), serverHttpRequest.getPath().value(),
                HttpStatus.CONFLICT, DUPLICATE_DATA_FOUND_DESCRIPTION, duplicateDataFoundException);
        logError(apiError);
        return buildResponseEntity(apiError);
    }


    /**
     * ResourceNotFoundException exception handler.
     *
     * @param ex {@link ResourceNotFoundException}
     * @return responseEntity with status set to 404 and payload set to {@link ApiError}
     */
    @ExceptionHandler(value = ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ApiError> handleResourceNotFoundException(ResourceNotFoundException ex, ServerHttpRequest request) {
        ApiError apiError = new ApiError(request.getMethod(), request.getPath().value(), HttpStatus.NOT_FOUND, RESOURCE_NOT_FOUND_DESCRIPTION, ex);
        logError(apiError);
        return buildResponseEntity(apiError);
    }

    /**
     * Unauthorized exception handler.
     *
     * @param unauthorizedException {@link UnauthorizedException}
     * @return responseEntity with status set to 401 and payload set to {@link ApiError}
     */
    @ExceptionHandler(value = UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ApiError> handleUnauthorizedException(UnauthorizedException unauthorizedException,
                                                                ServerHttpRequest serverHttpRequest) {

        ApiError apiError = new ApiError(serverHttpRequest.getMethod(), serverHttpRequest.getPath().value(),
                HttpStatus.UNAUTHORIZED, UNAUTHORIZED_DESCRIPTION, unauthorizedException);
        logError(apiError);
        return buildResponseEntity(apiError);
    }

    /**
     * Forbidden exception handler.
     *
     * @param forbiddenException {@link ForbiddenException}
     * @return responseEntity with status set to 403 and payload set to {@link ApiError}
     */
    @ExceptionHandler(value = ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<ApiError> handleForbiddenException(ForbiddenException forbiddenException,
                                                             ServerHttpRequest serverHttpRequest) {

        ApiError apiError = new ApiError(serverHttpRequest.getMethod(), serverHttpRequest.getPath().value(),
                HttpStatus.FORBIDDEN, FORBIDDEN_DESCRIPTION, forbiddenException);
        logError(apiError);
        return buildResponseEntity(apiError);
    }

    /**
     * Internal server error exception handler.
     *
     * @param internalServerException {@link InternalServerException}
     * @return responseEntity with status set to 500 and payload set to {@link ApiError}
     */
    @ExceptionHandler(value = InternalServerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiError> handleInternalServerException(InternalServerException internalServerException,
                                                                  ServerHttpRequest serverHttpRequest) {

        ApiError apiError = new ApiError(serverHttpRequest.getMethod(), serverHttpRequest.getPath().value(),
                HttpStatus.INTERNAL_SERVER_ERROR, internalServerException);
        logError(apiError);

        // Details of the error have been logged, however, as we are potentially dealing with an exception thrown by a
        // third party library for which we have no control over the exception message, to prevent leaking system
        // details to the client suppress the debug message containing this information.
        apiError.setDebugMessage(null);

        return buildResponseEntity(apiError);
    }

    /**
     * Constraint violation exception handler.
     *
     * @param constraintViolationException {@link ConstraintViolationException}
     * @return responseEntity with status set to 400 and payload set to {@link ApiError} holding a list of {@link ApiValidationError}
     */
    @ExceptionHandler(value = ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiError> handleConstraintViolationException(ConstraintViolationException constraintViolationException,
                                                                       ServerHttpRequest serverHttpRequest) {

        ApiError apiError = new ApiError(serverHttpRequest.getMethod(), serverHttpRequest.getPath().value(),
                HttpStatus.BAD_REQUEST, VALIDATION_ERROR_DESCRIPTION, constraintViolationException);

        List<ApiSubError> validationSubErrors = new ArrayList<>();
        ExceptionUtils.constructValidationErrorObject(constraintViolationException, validationSubErrors);

        return buildResponseEntity(finaliseAPIError(apiError, validationSubErrors));
    }

    /**
     * Bind exception handler. This catches exceptions thrown by parameter validation e.g. parameter failing regex constraint.
     *
     * @param bindException {@link BindException}
     * @return responseEntity with status set to 400 and payload set to {@link ApiError} holding a list of {@link ApiValidationError}
     */
    @ExceptionHandler(value = BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiError> handleBindException(BindException bindException,
                                                        ServerHttpRequest serverHttpRequest) {

        ApiError apiError = new ApiError(serverHttpRequest.getMethod(), serverHttpRequest.getPath().value(),
                HttpStatus.BAD_REQUEST, VALIDATION_ERROR_DESCRIPTION, bindException);

        List<ApiSubError> validationSubErrors = new ArrayList<>();

        for (FieldError fieldError : bindException.getFieldErrors()) {
            ApiValidationError validationError = new ApiValidationError(fieldError.getField(), fieldError.getRejectedValue(), fieldError.getDefaultMessage());
            validationSubErrors.add(validationError);
        }

        return buildResponseEntity(finaliseAPIError(apiError, validationSubErrors));
    }

    /**
     * Server Web Input exception handler. This catches exceptions thrown 'contract' validation, e.g. mandatory parameter has not been supplied.
     *
     * @param serverWebInputException {@link ServerWebInputException}
     * @return responseEntity with status set to 400 and payload set to {@link ApiError} holding a list of {@link ApiValidationError}
     */
    @ExceptionHandler(value = ServerWebInputException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiError> handleServerWebInputException(ServerWebInputException serverWebInputException,
                                                                  ServerHttpRequest serverHttpRequest) {

        ApiError apiError = new ApiError(serverHttpRequest.getMethod(), serverHttpRequest.getPath().value(),
                HttpStatus.BAD_REQUEST, VALIDATION_ERROR_DESCRIPTION, serverWebInputException);

        logError(apiError);

        // If the root cause is due to a invalid format exception, add a validation sub error with the field name for which it failed
        if (serverWebInputException.getCause() instanceof DecodingException) {
            DecodingException decodingException = (DecodingException) serverWebInputException.getCause();
            if (decodingException.getCause() instanceof InvalidFormatException || decodingException.getMostSpecificCause() instanceof MismatchedInputException) {
                InvalidFormatException invalidFormatException = (InvalidFormatException) decodingException.getCause();
                List<JsonMappingException.Reference> references = invalidFormatException.getPath();
                StringBuilder path = new StringBuilder();
                for (JsonMappingException.Reference reference : references) {
                    path.append(reference.getFieldName()).append(".");
                }
                apiError.setDebugMessage(null);
                if (Objects.requireNonNull(decodingException.getMessage()).contains("only \"true\" or \"false\" recognized")) {
                    String fieldName = path.length() > 0 ? path.substring(0, path.length() - 1) : "";
                    ApiValidationError validationError = new ApiValidationError(fieldName, decodingException.getMessage().substring(
                            decodingException.getMessage().indexOf("String ") + 8, decodingException.getMessage().indexOf("\": only")), fieldName + " should be true or false");
                    apiError.setSubErrors(Collections.singletonList(validationError));
                    return buildResponseEntity(apiError);
                }
                ApiValidationError validationError = new ApiValidationError(path.length() > 0 ? path.substring(0, path.length() - 1) : "", invalidFormatException.getValue(), "Invalid format");
                apiError.setSubErrors(Collections.singletonList(validationError));
            }
        }

        return buildResponseEntity(apiError);
    }

    private ApiError finaliseAPIError(ApiError apiError, List<ApiSubError> validationSubErrors) {
        if (!validationSubErrors.isEmpty()) {
            apiError.setSubErrors(validationSubErrors);
        }

        logError(apiError);

        // Details of the error have been logged, however, as we are dealing with an exception thrown by a third party
        // library for which we have no control over the exception message, to prevent leaking system details to the
        // client suppress the debug message containing this information.
        apiError.setDebugMessage(null);

        return apiError;
    }

    /**
     * Constraint violation exception handler.
     *
     * @param methodArgumentNotValidException {@link MethodArgumentNotValidException}
     * @return responseEntity with status set to 400 and payload set to {@link ApiError} holding a list of {@link ApiValidationError}
     */
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiError> handleMethodArgumentNotValidException(MethodArgumentNotValidException methodArgumentNotValidException,
                                                                          ServerHttpRequest serverHttpRequest) {

        ApiError apiError = new ApiError(serverHttpRequest.getMethod(), serverHttpRequest.getPath().value(),
                HttpStatus.BAD_REQUEST, VALIDATION_ERROR_DESCRIPTION, methodArgumentNotValidException);

        List<ApiSubError> validationSubErrors = new ArrayList<>();
        for (FieldError fieldError : methodArgumentNotValidException.getBindingResult().getFieldErrors()) {

            ApiValidationError validationError = new ApiValidationError(fieldError.getField(),
                    fieldError.getRejectedValue(), fieldError.getDefaultMessage());
            validationSubErrors.add(validationError);
        }

        if (!validationSubErrors.isEmpty()) {
            apiError.setSubErrors(validationSubErrors);
        }

        logError(apiError);

        return buildResponseEntity(apiError);
    }

    /**
     * Exception handler when the given type of data is a mismatch for controller. For example 123 passed for boolean type parameter.
     *
     * @param methodArgumentTypeMismatchException {@link MethodArgumentTypeMismatchException}
     * @param serverHttpRequest                   {@link ServerHttpRequest}
     * @return responseEntity with status set to 400 and payload set to {@link ApiError}
     */
    @ExceptionHandler(value = MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiError> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException methodArgumentTypeMismatchException,
                                                                              ServerHttpRequest serverHttpRequest) {

        ApiError apiError = new ApiError(serverHttpRequest.getMethod(), serverHttpRequest.getPath().value(),
                HttpStatus.BAD_REQUEST, VALIDATION_ERROR_DESCRIPTION, methodArgumentTypeMismatchException);

        ApiValidationError validationError = new ApiValidationError(methodArgumentTypeMismatchException.getName(),
                methodArgumentTypeMismatchException.getValue(), methodArgumentTypeMismatchException.getLocalizedMessage());
        apiError.setSubErrors(Collections.singletonList(validationError));

        logError(apiError);

        return buildResponseEntity(apiError);
    }

    //    /**
//     * Servlet request binding exception handler. The ServletRequestBindingException is thrown when a mandatory request
//     * parameter is not sent in the request.
//     *
//     * @param webExchangeBindException {@link WebExchangeBindException}
//     * @return responseEntity with status set to 400 and payload set to {@link ApiError}
//     */
    @ExceptionHandler(value = WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiError> handleServletRequestBindingException(WebExchangeBindException webExchangeBindException,
                                                                         ServerHttpRequest serverHttpRequest) {
        ApiError apiError = new ApiError(serverHttpRequest.getMethod(), serverHttpRequest.getPath().value(),
                HttpStatus.BAD_REQUEST, VALIDATION_ERROR_DESCRIPTION, webExchangeBindException);
        if (!webExchangeBindException.getGlobalErrors().isEmpty()) {
            ApiValidationError validationError = new ApiValidationError("request body",
                    null, webExchangeBindException.getGlobalError().getDefaultMessage());
            apiError.setSubErrors(Collections.singletonList(validationError));
        }

        List<ApiSubError> validationSubErrors = new ArrayList<>();
        for (FieldError fieldError : webExchangeBindException.getFieldErrors()) {
            Object  rejectedVal = fieldError.getRejectedValue() != null ? fieldError.getRejectedValue() instanceof LocalDate ? ((LocalDate)fieldError.getRejectedValue()).format(DateTimeFormatter.ofPattern("dd-MM-yyyy")): fieldError.getRejectedValue():null;
            ApiValidationError validationError = new ApiValidationError(fieldError.getField(),
                    Objects.requireNonNullElse(rejectedVal, "").toString(), fieldError.getDefaultMessage());
            validationSubErrors.add(validationError);
        }

        if (!validationSubErrors.isEmpty()) {
            apiError.setSubErrors(validationSubErrors);
        }

        logError(apiError);
        // WebExchangeBindException error message is too verbose and leaks information with regards to the underlying
        // framework used, we have logged the verbose error message so now set debug message to null so that it is not
        // returned to the client
        apiError.setDebugMessage(null);

        return buildResponseEntity(apiError);
    }

    /**
     * Bad request exception handler to handle.
     *
     * @param badRequestException {@link BadRequestException} Caught exception.
     * @return responseEntity with status set to 500 and payload set to {@link ApiError}
     */
    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiError> handleBadRequestException(BadRequestException badRequestException, ServerHttpRequest serverHttpRequest) {

        ApiError apiError;
        if (null == badRequestException.getApiError()) {
            apiError = new ApiError(serverHttpRequest.getMethod(), serverHttpRequest.getPath().value(),
                    HttpStatus.BAD_REQUEST, BAD_REQUEST_DESCRIPTION, badRequestException);
        } else {
            apiError = badRequestException.getApiError();
        }

        logError(apiError);
        return buildResponseEntity(apiError);
    }

    /**
     * Handler to handle and log errors for exceptions that are due to a failing http call.
     *
     * @param exception Caught exception.
     * @return responseEntity with status set to 500 and payload set to {@link ApiError}
     */
    @ExceptionHandler(HttpStatusCodeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiError> handleHttpClientException(HttpStatusCodeException exception, ServerHttpRequest serverHttpRequest) {

        ApiError apiError = new ApiError(serverHttpRequest.getMethod(), serverHttpRequest.getPath().value(),
                HttpStatus.INTERNAL_SERVER_ERROR, exception);
        if (exception.getResponseBodyAsString() != null) {
            apiError.setDebugMessage(exception.getLocalizedMessage() + ", response body is " + exception.getResponseBodyAsString());
        }
        logError(apiError, exception);

        // Details of the error have been logged, however, as we are dealing with an exception thrown by a third party
        // library for which we have no control over the exception message, to prevent leaking system details to the
        // client suppress the debug message containing this information.
        apiError.setDebugMessage(null);

        return buildResponseEntity(apiError);
    }

    /**
     * Handler to handle unexpected errors that haven't been handled and thrown as custom exception
     *
     * @param exception Caught exception.
     * @return responseEntity with status set to 500 and payload set to {@link ApiError}
     */
    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiError> handleUnexpectedException(Exception exception, ServerHttpRequest serverHttpRequest) {

        ApiError apiError = new ApiError(serverHttpRequest.getMethod(), serverHttpRequest.getPath().value(),
                HttpStatus.INTERNAL_SERVER_ERROR, exception);
        logError(apiError, exception);

        // Details of the error have been logged, however, as we are dealing with an exception thrown by a third party
        // library for which we have no control over the exception message, to prevent leaking system details to the
        // client suppress the debug message containing this information.
        apiError.setDebugMessage(null);

        return buildResponseEntity(apiError);
    }

}
