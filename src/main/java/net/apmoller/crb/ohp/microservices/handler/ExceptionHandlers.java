package net.apmoller.crb.ohp.microservices.handler;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import lombok.extern.slf4j.Slf4j;
import net.apmoller.crb.ohp.microservices.exception.*;
import net.apmoller.crb.ohp.microservices.model.ApiError;
import net.apmoller.crb.ohp.microservices.model.ApiSubError;
import net.apmoller.crb.ohp.microservices.model.ApiValidationError;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ElementKind;
import javax.validation.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.apmoller.crb.ohp.microservices.util.ExceptionUtils.*;


/**
 * Set of generic error handlers to be used by Spring Boot RestController classes.
 */
@RestControllerAdvice
@Slf4j
public class ExceptionHandlers {

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
                                                                ServletWebRequest servletWebRequest) {

        ApiError apiError = new ApiError(servletWebRequest.getHttpMethod(), servletWebRequest.getRequest().getServletPath(),
                HttpStatus.NOT_FOUND, DATA_NOT_FOUND_DESCRIPTION, dataNotFoundException);
        logError(apiError);
        return buildResponseEntity(apiError);
    }

    @ExceptionHandler(value = ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ApiError> handleResourceNotFoundException(ResourceNotFoundException ex, ServletWebRequest request) {
        ApiError apiError = new ApiError(request.getHttpMethod(),
                request.getRequest().getRequestURI(),
                HttpStatus.NOT_FOUND,
                RESOURCE_NOT_FOUND_DESCRIPTION,
                ex);

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
                                                                ServletWebRequest servletWebRequest) {

        ApiError apiError = new ApiError(servletWebRequest.getHttpMethod(), servletWebRequest.getRequest().getServletPath(),
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
                                                             ServletWebRequest servletWebRequest) {

        ApiError apiError = new ApiError(servletWebRequest.getHttpMethod(), servletWebRequest.getRequest().getServletPath(),
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
                                                                  ServletWebRequest servletWebRequest) {

        ApiError apiError = new ApiError(servletWebRequest.getHttpMethod(), servletWebRequest.getRequest().getServletPath(),
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
                                                                       ServletWebRequest servletWebRequest) {

        ApiError apiError = new ApiError(servletWebRequest.getHttpMethod(), servletWebRequest.getRequest().getServletPath(),
                HttpStatus.BAD_REQUEST, VALIDATION_ERROR_DESCRIPTION, constraintViolationException);

        List<ApiSubError> validationSubErrors = new ArrayList<>();
        for (ConstraintViolation constraintViolation : constraintViolationException.getConstraintViolations()) {
            for (Path.Node propertyPath : constraintViolation.getPropertyPath()) {
                // In the case we are validating an object we are interested in the property name, for a validated
                // method call we are interested in the parameter name
                if (propertyPath.getKind() == ElementKind.PROPERTY || propertyPath.getKind() == ElementKind.PARAMETER) {
                    ApiValidationError validationError = new ApiValidationError(propertyPath.getName(),
                            constraintViolation.getInvalidValue(), constraintViolation.getMessage());
                    validationSubErrors.add(validationError);
                    break;
                }
            }
        }

        return buildResponseEntity(finaliseAPIError(apiError, validationSubErrors));
    }

    /**
     * Bind exception handler.
     *
     * @param bindException {@link BindException}
     * @return responseEntity with status set to 400 and payload set to {@link ApiError} holding a list of {@link ApiValidationError}
     */
    @ExceptionHandler(value = BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiError> handleBindException(BindException bindException,
                                                        ServletWebRequest servletWebRequest) {

        ApiError apiError = new ApiError(servletWebRequest.getHttpMethod(), servletWebRequest.getRequest().getServletPath(),
                HttpStatus.BAD_REQUEST, VALIDATION_ERROR_DESCRIPTION, bindException);

        List<ApiSubError> validationSubErrors = new ArrayList<>();

        for (FieldError fieldError : bindException.getFieldErrors()) {
            ApiValidationError validationError = new ApiValidationError(fieldError.getField(), fieldError.getRejectedValue(), fieldError.getDefaultMessage());
            validationSubErrors.add(validationError);
        }

        return buildResponseEntity(finaliseAPIError(apiError, validationSubErrors));
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
                                                                          ServletWebRequest servletWebRequest) {

        ApiError apiError = new ApiError(servletWebRequest.getHttpMethod(), servletWebRequest.getRequest().getServletPath(),
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
        // Details of the error have been logged, however, as we are dealing with an exception thrown by a third party
        // library for which we have no control over the exception message, to prevent leaking system details to the
        // client suppress the debug message containing this information.
        apiError.setDebugMessage(null);

        return buildResponseEntity(apiError);
    }

    /**
     * Exception handler when the given type of data is a mismatch for controller. For example 123 passed for boolean type parameter.
     *
     * @param methodArgumentTypeMismatchException {@link MethodArgumentTypeMismatchException}
     * @param servletWebRequest                   {@link ServletWebRequest}
     * @return responseEntity with status set to 400 and payload set to {@link ApiError}
     */
    @ExceptionHandler(value = MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiError> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException methodArgumentTypeMismatchException,
                                                                              ServletWebRequest servletWebRequest) {

        ApiError apiError = new ApiError(servletWebRequest.getHttpMethod(), servletWebRequest.getRequest().getServletPath(),
                HttpStatus.BAD_REQUEST, VALIDATION_ERROR_DESCRIPTION, methodArgumentTypeMismatchException);

        ApiValidationError validationError = new ApiValidationError(methodArgumentTypeMismatchException.getName(),
                methodArgumentTypeMismatchException.getValue(), methodArgumentTypeMismatchException.getLocalizedMessage());
        apiError.setSubErrors(Collections.singletonList(validationError));

        logError(apiError);

        return buildResponseEntity(apiError);
    }

    /**
     * Servlet request binding exception handler. The ServletRequestBindingException is thrown when a mandatory request
     * parameter is not sent in the request.
     *
     * @param servletRequestBindingException {@link ServletRequestBindingException}
     * @return responseEntity with status set to 400 and payload set to {@link ApiError}
     */
    @ExceptionHandler(value = ServletRequestBindingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiError> handleServletRequestBindingException(ServletRequestBindingException servletRequestBindingException,
                                                                         ServletWebRequest servletWebRequest) {

        ApiError apiError = new ApiError(servletWebRequest.getHttpMethod(), servletWebRequest.getRequest().getServletPath(),
                HttpStatus.BAD_REQUEST, VALIDATION_ERROR_DESCRIPTION, servletRequestBindingException);

        logError(apiError);

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
    public ResponseEntity<ApiError> handleBadRequestException(BadRequestException badRequestException, ServletWebRequest servletWebRequest) {
        ApiError apiError;
        if (null == badRequestException.getApiError()) {
            apiError = new ApiError(servletWebRequest.getHttpMethod(),
                    servletWebRequest.getRequest().getRequestURI(),
                    HttpStatus.BAD_REQUEST,
                    BAD_REQUEST_DESCRIPTION,
                    badRequestException);
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
    public ResponseEntity<ApiError> handleHttpClientException(HttpStatusCodeException exception, ServletWebRequest servletWebRequest) {

        ApiError apiError = new ApiError(servletWebRequest.getHttpMethod(), servletWebRequest.getRequest().getServletPath(),
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
    public ResponseEntity<ApiError> handleUnexpectedException(Exception exception, ServletWebRequest servletWebRequest) {

        ApiError apiError = new ApiError(servletWebRequest.getHttpMethod(), servletWebRequest.getRequest().getServletPath(),
                HttpStatus.INTERNAL_SERVER_ERROR, exception);
        logError(apiError, exception);

        // Details of the error have been logged, however, as we are dealing with an exception thrown by a third party
        // library for which we have no control over the exception message, to prevent leaking system details to the
        // client suppress the debug message containing this information.
        apiError.setDebugMessage(null);

        return buildResponseEntity(apiError);
    }

    /**
     * Handler to handle HttpMessageNotReadableException errors that haven't been handled and thrown as custom exception
     *
     * @param httpMessageNotReadableException HttpMessageNotReadableException
     * @return responseEntity with status set to 400 and payload set to {@link ApiError}
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiError> httpMessageNotReadableException(HttpMessageNotReadableException httpMessageNotReadableException, ServletWebRequest servletWebRequest) {

        ApiError apiError = new ApiError(servletWebRequest.getHttpMethod(), servletWebRequest.getRequest().getServletPath(),
                HttpStatus.BAD_REQUEST, VALIDATION_ERROR_DESCRIPTION, httpMessageNotReadableException);
        logError(apiError, httpMessageNotReadableException);

        // If the root cause is due to a invalid format exception, add a validation sub error with the field name for which it failed
        if (httpMessageNotReadableException.getCause() instanceof InvalidFormatException) {
            InvalidFormatException invalidFormatException = (InvalidFormatException) httpMessageNotReadableException.getCause();
            List<JsonMappingException.Reference> references = invalidFormatException.getPath();
            StringBuilder path = new StringBuilder();
            for (JsonMappingException.Reference reference : references) {
                path.append(reference.getFieldName()).append(".");
            }
            apiError.setDebugMessage(null);
            ApiValidationError validationError = new ApiValidationError(path.length() > 0 ? path.substring(0, path.length() - 1) : "", invalidFormatException.getValue(), "Invalid format");
            apiError.setSubErrors(Collections.singletonList(validationError));
        }

        return buildResponseEntity(apiError);
    }

    /**
     * HttpMediaTypeNotSupportedException exception handler.
     *
     * @param httpMediaTypeNotSupportedException {@link HttpMediaTypeNotSupportedException}
     * @return responseEntity with status set to 415 and payload set to {@link ApiError}
     */
    @ExceptionHandler(value = HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ResponseEntity<ApiError> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException httpMediaTypeNotSupportedException,
                                                                             ServletWebRequest servletWebRequest) {
        ApiError apiError = new ApiError(servletWebRequest.getHttpMethod(), servletWebRequest.getRequest().getServletPath(),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE, UNSUPPORTED_MEDIA_TYPE_DESCRIPTION, httpMediaTypeNotSupportedException);
        logError(apiError);
        return buildResponseEntity(apiError);
    }

}
