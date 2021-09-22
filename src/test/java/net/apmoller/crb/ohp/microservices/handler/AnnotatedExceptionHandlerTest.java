package net.apmoller.crb.ohp.microservices.handler;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import net.apmoller.crb.ohp.microservices.exception.*;
import net.apmoller.crb.ohp.microservices.model.ApiError;
import net.apmoller.crb.ohp.microservices.model.ApiSubError;
import net.apmoller.crb.ohp.microservices.model.ApiValidationError;
import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;
import org.springframework.core.MethodParameter;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ServerWebInputException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.executable.ExecutableValidator;
import java.io.IOException;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for testing generic error handlers for Spring Boot micro services
 */
public class AnnotatedExceptionHandlerTest {

    private AnnotatedExceptionHandler exceptionHandlers = new AnnotatedExceptionHandler();

    private MockServerHttpRequest serverHttpRequest;

    private static final String FIELD_DESC_CONTENT_TYPE = "content type";
    private static final String FIELD_DESC_HAS_RESPONSE_BODY = "has response body";
    private static final String FIELD_DESC_API_ERROR_RESPONSE = "api error response";
    private static final String FIELD_DESC_API_ERROR_STATUS = "api error status";
    private static final String FIELD_DESC_API_ERROR_TIMESTAMP = "api error timestamp";
    private static final String FIELD_DESC_API_ERROR_MESSAGE = "api error message";
    private static final String FIELD_DESC_API_ERROR_DEBUG_MESSAGE = "api error debug message";
    private static final String FIELD_DESC_API_ERROR_SUB_ERRORS = "api error sub errors";
    private static final String FIELD_DESC_API_ERROR_SUB_ERROR_COUNT = "api error sub error count";
    private static final String FIELD_DESC_VALIDATION_ERROR_COUNT = "validation error count";

    @Before
    public void setUp() {
        this.serverHttpRequest = MockServerHttpRequest
                .method(HttpMethod.POST, "aUrl")
                .build();
    }

    @Test
    public void noDataFoundShouldReturn404AndApiError() {

        // given
        DataNotFoundException dataNotFoundException = new DataNotFoundException("No Data is available for given request");

        // when
        ResponseEntity<ApiError> responseEntity = exceptionHandlers.handleDataNotFoundException(dataNotFoundException, this.serverHttpRequest);

        then(responseEntity).describedAs("data not found response").isNotNull();
        then(responseEntity.getStatusCode()).describedAs("data not found response status").isEqualTo(HttpStatus.NOT_FOUND);
        then(responseEntity.getHeaders().getContentType()).describedAs(FIELD_DESC_CONTENT_TYPE).isEqualTo(MediaType.APPLICATION_JSON);
        then(responseEntity.hasBody()).describedAs(FIELD_DESC_HAS_RESPONSE_BODY).isEqualTo(true);

        ApiError apiError = responseEntity.getBody();
        then(apiError).describedAs(FIELD_DESC_API_ERROR_RESPONSE).isNotNull();
        then(apiError.getStatus()).describedAs(FIELD_DESC_API_ERROR_STATUS).isEqualTo(HttpStatus.NOT_FOUND.value());
        then(apiError.getTimestamp()).describedAs(FIELD_DESC_API_ERROR_TIMESTAMP).isNotNull();
        then(apiError.getMessage()).describedAs(FIELD_DESC_API_ERROR_MESSAGE).isEqualTo("Data not found");
        then(apiError.getDebugMessage()).describedAs(FIELD_DESC_API_ERROR_DEBUG_MESSAGE).isEqualTo(dataNotFoundException.getLocalizedMessage());
        then(apiError.getSubErrors()).describedAs(FIELD_DESC_API_ERROR_SUB_ERRORS).isNull();

    }

    @Test
    public void unauthorizedShouldReturn401AndApiError() {
        // given
        UnauthorizedException unauthorizedException = new UnauthorizedException("User un-authorized");

        // when
        ResponseEntity<ApiError> responseEntity = exceptionHandlers.handleUnauthorizedException(unauthorizedException, this.serverHttpRequest);

        then(responseEntity).describedAs("unauthorized response").isNotNull();
        then(responseEntity.getStatusCode()).describedAs("unauthorized response status").isEqualTo(HttpStatus.UNAUTHORIZED);
        then(responseEntity.getHeaders().getContentType()).describedAs(FIELD_DESC_CONTENT_TYPE).isEqualTo(MediaType.APPLICATION_JSON);
        then(responseEntity.hasBody()).describedAs(FIELD_DESC_HAS_RESPONSE_BODY).isEqualTo(true);

        ApiError apiError = responseEntity.getBody();
        then(apiError).describedAs(FIELD_DESC_API_ERROR_RESPONSE).isNotNull();
        then(apiError.getStatus()).describedAs(FIELD_DESC_API_ERROR_STATUS).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        then(apiError.getTimestamp()).describedAs(FIELD_DESC_API_ERROR_TIMESTAMP).isNotNull();
        then(apiError.getMessage()).describedAs(FIELD_DESC_API_ERROR_MESSAGE).isEqualTo("Unauthorized");
        then(apiError.getDebugMessage()).describedAs(FIELD_DESC_API_ERROR_DEBUG_MESSAGE).isEqualTo(unauthorizedException.getLocalizedMessage());
        then(apiError.getSubErrors()).describedAs(FIELD_DESC_API_ERROR_SUB_ERRORS).isNull();

    }

    @Test
    public void forbiddenShouldReturn403AndApiError() {

        // given
        ForbiddenException forbiddenException = new ForbiddenException("Forbidden");

        // when
        ResponseEntity<ApiError> responseEntity = exceptionHandlers.handleForbiddenException(forbiddenException, this.serverHttpRequest);

        then(responseEntity).describedAs("forbidden response").isNotNull();
        then(responseEntity.getStatusCode()).describedAs("forbidden response status").isEqualTo(HttpStatus.FORBIDDEN);
        then(responseEntity.getHeaders().getContentType()).describedAs(FIELD_DESC_CONTENT_TYPE).isEqualTo(MediaType.APPLICATION_JSON);
        then(responseEntity.hasBody()).describedAs(FIELD_DESC_HAS_RESPONSE_BODY).isEqualTo(true);

        ApiError apiError = responseEntity.getBody();
        then(apiError).describedAs(FIELD_DESC_API_ERROR_RESPONSE).isNotNull();
        then(apiError.getStatus()).describedAs(FIELD_DESC_API_ERROR_STATUS).isEqualTo(HttpStatus.FORBIDDEN.value());
        then(apiError.getTimestamp()).describedAs(FIELD_DESC_API_ERROR_TIMESTAMP).isNotNull();
        then(apiError.getMessage()).describedAs(FIELD_DESC_API_ERROR_MESSAGE).isEqualTo("Forbidden");
        then(apiError.getDebugMessage()).describedAs(FIELD_DESC_API_ERROR_DEBUG_MESSAGE).isEqualTo(forbiddenException.getLocalizedMessage());
        then(apiError.getSubErrors()).describedAs(FIELD_DESC_API_ERROR_SUB_ERRORS).isNull();

    }

    @Test
    public void internalServerExceptionShouldReturn500AndApiError() {
        // given
        InternalServerException internalServerException = new InternalServerException("Some unexpected error");

        // when
        ResponseEntity<ApiError> responseEntity = exceptionHandlers.handleInternalServerException(internalServerException, this.serverHttpRequest);

        then(responseEntity).describedAs("internal server error response").isNotNull();
        then(responseEntity.getStatusCode()).describedAs("internal server error response status").isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        then(responseEntity.getHeaders().getContentType()).describedAs(FIELD_DESC_CONTENT_TYPE).isEqualTo(MediaType.APPLICATION_JSON);
        then(responseEntity.hasBody()).describedAs(FIELD_DESC_HAS_RESPONSE_BODY).isEqualTo(true);

        ApiError apiError = responseEntity.getBody();
        then(apiError).describedAs(FIELD_DESC_API_ERROR_RESPONSE).isNotNull();
        then(apiError.getStatus()).describedAs(FIELD_DESC_API_ERROR_STATUS).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        then(apiError.getTimestamp()).describedAs(FIELD_DESC_API_ERROR_TIMESTAMP).isNotNull();
        then(apiError.getMessage()).describedAs(FIELD_DESC_API_ERROR_MESSAGE).isEqualTo("Unexpected error");
        then(apiError.getDebugMessage()).describedAs(FIELD_DESC_API_ERROR_DEBUG_MESSAGE).isNull();
        then(apiError.getSubErrors()).describedAs(FIELD_DESC_API_ERROR_SUB_ERRORS).isNull();
    }

    @Test
    public void unhandledExceptionShouldReturn500AndApiError() {
        // given
        Exception exception = new IOException("Some unexpected error that hasn't been handled.");

        // when
        ResponseEntity<ApiError> responseEntity = exceptionHandlers.handleUnexpectedException(exception, this.serverHttpRequest);

        then(responseEntity).describedAs("internal server error response").isNotNull();
        then(responseEntity.getStatusCode()).describedAs("internal server error response status").isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        then(responseEntity.getHeaders().getContentType()).describedAs(FIELD_DESC_CONTENT_TYPE).isEqualTo(MediaType.APPLICATION_JSON);
        then(responseEntity.hasBody()).describedAs(FIELD_DESC_HAS_RESPONSE_BODY).isEqualTo(true);

        ApiError apiError = responseEntity.getBody();
        then(apiError).describedAs(FIELD_DESC_API_ERROR_RESPONSE).isNotNull();
        then(apiError.getStatus()).describedAs(FIELD_DESC_API_ERROR_STATUS).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        then(apiError.getTimestamp()).describedAs(FIELD_DESC_API_ERROR_TIMESTAMP).isNotNull();
        then(apiError.getMessage()).describedAs(FIELD_DESC_API_ERROR_MESSAGE).isEqualTo("Unexpected error");
        then(apiError.getDebugMessage()).describedAs(FIELD_DESC_API_ERROR_DEBUG_MESSAGE).isNull();
        then(apiError.getSubErrors()).describedAs(FIELD_DESC_API_ERROR_SUB_ERRORS).isNull();
    }

    @Test
    public void httpClientExceptionShouldReturn500AndApiError() {
        // given
        HttpStatusCodeException exception = new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad request");

        // when
        ResponseEntity<ApiError> responseEntity = exceptionHandlers.handleHttpClientException(exception, this.serverHttpRequest);

        then(responseEntity).describedAs("internal server error response").isNotNull();
        then(responseEntity.getStatusCode()).describedAs("internal server error response status").isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        then(responseEntity.getHeaders().getContentType()).describedAs(FIELD_DESC_CONTENT_TYPE).isEqualTo(MediaType.APPLICATION_JSON);
        then(responseEntity.hasBody()).describedAs(FIELD_DESC_HAS_RESPONSE_BODY).isEqualTo(true);

        ApiError apiError = responseEntity.getBody();
        then(apiError).describedAs(FIELD_DESC_API_ERROR_RESPONSE).isNotNull();
        then(apiError.getStatus()).describedAs(FIELD_DESC_API_ERROR_STATUS).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        then(apiError.getTimestamp()).describedAs(FIELD_DESC_API_ERROR_TIMESTAMP).isNotNull();
        then(apiError.getMessage()).describedAs(FIELD_DESC_API_ERROR_MESSAGE).isEqualTo("Unexpected error");
        then(apiError.getDebugMessage()).describedAs(FIELD_DESC_API_ERROR_DEBUG_MESSAGE).isNull();
        then(apiError.getSubErrors()).describedAs(FIELD_DESC_API_ERROR_SUB_ERRORS).isNull();
    }

    @Test
    public void httpClientExceptionWithBodyShouldReturn500AndApiError() {
        // given
        HttpStatusCodeException exception = new HttpClientErrorException(HttpStatus.BAD_REQUEST,
                "Bad request", "{\"message\":\"service name must not be null\"".getBytes(), Charset.defaultCharset());

        // when
        ResponseEntity<ApiError> responseEntity = exceptionHandlers.handleHttpClientException(exception, this.serverHttpRequest);

        then(responseEntity).describedAs("internal server error response").isNotNull();
        then(responseEntity.getStatusCode()).describedAs("internal server error response status").isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        then(responseEntity.getHeaders().getContentType()).describedAs(FIELD_DESC_CONTENT_TYPE).isEqualTo(MediaType.APPLICATION_JSON);
        then(responseEntity.hasBody()).describedAs(FIELD_DESC_HAS_RESPONSE_BODY).isEqualTo(true);

        ApiError apiError = responseEntity.getBody();
        then(apiError).describedAs(FIELD_DESC_API_ERROR_RESPONSE).isNotNull();
        then(apiError.getStatus()).describedAs(FIELD_DESC_API_ERROR_STATUS).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        then(apiError.getTimestamp()).describedAs(FIELD_DESC_API_ERROR_TIMESTAMP).isNotNull();
        then(apiError.getMessage()).describedAs(FIELD_DESC_API_ERROR_MESSAGE).isEqualTo("Unexpected error");
        then(apiError.getDebugMessage()).describedAs(FIELD_DESC_API_ERROR_DEBUG_MESSAGE).isNull();
        then(apiError.getSubErrors()).describedAs(FIELD_DESC_API_ERROR_SUB_ERRORS).isNull();
    }

    @Test
    public void badRequestExceptionShouldReturn400AndApiError() {
        // given
        BadRequestException badRequestException = new BadRequestException("Something wasn't right in the request");

        // when
        ResponseEntity<ApiError> responseEntity = exceptionHandlers.handleBadRequestException(badRequestException, this.serverHttpRequest);

        then(responseEntity).describedAs("bad request error response").isNotNull();
        then(responseEntity.getStatusCode()).describedAs("bad request response status").isEqualTo(HttpStatus.BAD_REQUEST);
        then(responseEntity.getHeaders().getContentType()).describedAs(FIELD_DESC_CONTENT_TYPE).isEqualTo(MediaType.APPLICATION_JSON);
        then(responseEntity.hasBody()).describedAs(FIELD_DESC_HAS_RESPONSE_BODY).isEqualTo(true);

        ApiError apiError = responseEntity.getBody();
        then(apiError).describedAs(FIELD_DESC_API_ERROR_RESPONSE).isNotNull();
        then(apiError.getStatus()).describedAs(FIELD_DESC_API_ERROR_STATUS).isEqualTo(HttpStatus.BAD_REQUEST.value());
        then(apiError.getTimestamp()).describedAs(FIELD_DESC_API_ERROR_TIMESTAMP).isNotNull();
        then(apiError.getMessage()).describedAs(FIELD_DESC_API_ERROR_MESSAGE).isEqualTo("Bad Request");
        then(apiError.getDebugMessage()).describedAs(FIELD_DESC_API_ERROR_DEBUG_MESSAGE).isEqualTo(badRequestException.getLocalizedMessage());
        then(apiError.getSubErrors()).describedAs(FIELD_DESC_API_ERROR_SUB_ERRORS).isNull();
    }


    @Test
    public void badRequestExceptionWithApiErrorSetShouldReturn400AndApiError() {

        // given
        BadRequestException badRequestExceptionThrownElsewhere = new BadRequestException("I was thrown from elsewhere");
        HttpMethod method = HttpMethod.GET;
        String requestUri = "/path/for/request";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = "Bad Request";
        ApiError apiError = new ApiError(method, requestUri, status, message, badRequestExceptionThrownElsewhere);

        BadRequestException badRequestException = new BadRequestException(apiError);

        // when
        ResponseEntity<ApiError> responseEntity = exceptionHandlers.handleBadRequestException(badRequestException, this.serverHttpRequest);

        then(responseEntity).describedAs("bad request error response").isNotNull();
        then(responseEntity.getStatusCode()).describedAs("bad request response status").isEqualTo(HttpStatus.BAD_REQUEST);
        then(responseEntity.getHeaders().getContentType()).describedAs(FIELD_DESC_CONTENT_TYPE).isEqualTo(MediaType.APPLICATION_JSON);
        then(responseEntity.hasBody()).describedAs(FIELD_DESC_HAS_RESPONSE_BODY).isEqualTo(true);

        ApiError apiErrorFromException = responseEntity.getBody();
        then(apiErrorFromException).describedAs(FIELD_DESC_API_ERROR_RESPONSE).isNotNull();
        then(apiErrorFromException.getStatus()).describedAs(FIELD_DESC_API_ERROR_STATUS).isEqualTo(HttpStatus.BAD_REQUEST.value());
        then(apiErrorFromException.getTimestamp()).describedAs(FIELD_DESC_API_ERROR_TIMESTAMP).isNotNull();
        then(apiErrorFromException.getMessage()).describedAs(FIELD_DESC_API_ERROR_MESSAGE).isEqualTo("Bad Request");
        then(apiErrorFromException.getDebugMessage()).describedAs(FIELD_DESC_API_ERROR_DEBUG_MESSAGE).isEqualTo(badRequestExceptionThrownElsewhere.getLocalizedMessage());
        then(apiErrorFromException.getSubErrors()).describedAs(FIELD_DESC_API_ERROR_SUB_ERRORS).isNull();
    }

    @Test
    public void bindExceptionShouldReturn400AndApiError() {
        // given
        BindingResult bindingResult = mock(BindingResult.class);
        BindException bindException = new BindException(bindingResult);

        // when
        ResponseEntity<ApiError> responseEntity = exceptionHandlers.handleBindException(bindException, this.serverHttpRequest);

        // then
        then(responseEntity).isNotNull();
        then(responseEntity.getStatusCode()).describedAs("bad request response status").isEqualTo(HttpStatus.BAD_REQUEST);

        FieldError fieldError = new FieldError("Code", "CarrierCode", "Invalid Carrier Code");
        bindingResult = new BeanPropertyBindingResult("User", "ID");
        bindingResult.addError(fieldError);
        bindException = new BindException(bindingResult);

        // when
        responseEntity = exceptionHandlers.handleBindException(bindException, this.serverHttpRequest);

        // then
        then(responseEntity).isNotNull();
        ApiError apiError = responseEntity.getBody();
        then(apiError.getSubErrors()).isNotNull().isNotEmpty();
        then(apiError.getSubErrors().size()).isEqualTo(1);
    }

    @Test
    public void handleMethodArgumentNotValidExceptionShouldReturn400AndApiError() {
        // given
        MethodParameter methodParameter = mock(MethodParameter.class);
        when(methodParameter.getParameterIndex()).thenReturn(0);
        Executable executable = mock(Executable.class);
        when(executable.toGenericString()).thenReturn("Some Executable");
        when(methodParameter.getExecutable()).thenReturn(executable);
        BindingResult bindingResult = mock(BindingResult.class);
        MethodArgumentNotValidException methodArgumentNotValidException = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // when
        ResponseEntity<ApiError> responseEntity = exceptionHandlers.handleMethodArgumentNotValidException(methodArgumentNotValidException, this.serverHttpRequest);


        // then
        then(responseEntity).isNotNull();
        then(responseEntity.getStatusCode()).describedAs("bad request response status").isEqualTo(HttpStatus.BAD_REQUEST);

        FieldError fieldError = new FieldError("Code", "CarrierCode", "Invalid Carrier Code");
        bindingResult = new BeanPropertyBindingResult("User", "ID");
        bindingResult.addError(fieldError);
        methodArgumentNotValidException = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // when
        responseEntity = exceptionHandlers.handleBindException(methodArgumentNotValidException, this.serverHttpRequest);

        // then
        then(responseEntity).isNotNull();
        ApiError apiError =  responseEntity.getBody();
        then(apiError.getSubErrors()).isNotNull().isNotEmpty();
        then(apiError.getSubErrors().size()).isEqualTo(1);
        List<ApiValidationError> validationErrors = apiError.getSubErrors().stream()
                .filter(subError -> subError instanceof ApiValidationError)
                .map(subError -> (ApiValidationError) subError)
                .sorted(Comparator.comparing(ApiValidationError::getField))
                .collect(Collectors.toList());
        ApiValidationError firstValidationError = validationErrors.get(0);
        then(firstValidationError.getField()).describedAs("first validation error field name").isEqualTo("CarrierCode");
        then(firstValidationError.getRejectedValue()).describedAs("first validation error rejected value").isNull();
        then(firstValidationError.getMessage()).describedAs("first validation error validation message").isEqualTo("Invalid Carrier Code");

    }


    @Test
    public void invalidTypeShouldReturn400AndApiError() {
        // given
        MethodParameter methodParameter = mock(MethodParameter.class);
        MethodArgumentTypeMismatchException typeMismatchException = new MethodArgumentTypeMismatchException("123", Boolean.TYPE, "isReefer",
                methodParameter, new IllegalArgumentException("Invalid value"));

        // when
        ResponseEntity<ApiError> responseEntity = exceptionHandlers.handleMethodArgumentTypeMismatchException(typeMismatchException, this.serverHttpRequest);

        // then
        then(responseEntity).isNotNull();
        then(responseEntity.getStatusCode()).describedAs("bad request response status").isEqualTo(HttpStatus.BAD_REQUEST);
        ApiError apiError = responseEntity.getBody();
        then(apiError).isNotNull();
        then(apiError.getSubErrors()).isNotNull().isNotEmpty();
        then(apiError.getSubErrors().size()).isEqualTo(1);
        List<ApiValidationError> validationErrors = apiError.getSubErrors().stream()
                .filter(subError -> subError instanceof ApiValidationError)
                .map(subError -> (ApiValidationError) subError)
                .sorted(Comparator.comparing(ApiValidationError::getField))
                .collect(Collectors.toList());
        ApiValidationError firstValidationError = validationErrors.get(0);
        then(firstValidationError.getField()).describedAs("first validation error field name").isEqualTo("isReefer");
        then(firstValidationError.getRejectedValue()).describedAs("first validation error rejected value").isEqualTo("123");
        then(firstValidationError.getMessage()).describedAs("first validation error validation message").isNotNull();
    }

    @Test
    public void finaliseAPIError_emptyListOfSubErrors() throws Exception {
        ApiError apiError = new ApiError(HttpMethod.GET, "test URI", HttpStatus.BAD_REQUEST);
        List<ApiSubError> subErrors = new ArrayList<>();
        apiError = Whitebox.invokeMethod(exceptionHandlers, "finaliseAPIError", apiError, subErrors);

        // then
        then(apiError).isNotNull();
        then(apiError.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        then(apiError.getDebugMessage()).isNull();
        then(apiError.getSubErrors()).isNull();
    }

    @Test
    public void finaliseAPIError_withListOfSubErrors() throws Exception {
        ApiError apiError = new ApiError(HttpMethod.GET, "test URI", HttpStatus.BAD_REQUEST);
        List<ApiSubError> subErrors = new ArrayList<>();
        ApiValidationError apiValidationError = new ApiValidationError("carrierCode", "MAEUX", "Invalid Carrier Code");
        subErrors.add(apiValidationError);
        apiError = Whitebox.invokeMethod(exceptionHandlers, "finaliseAPIError", apiError, subErrors);

        // then
        then(apiError).isNotNull();
        then(apiError.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        then(apiError.getDebugMessage()).describedAs(FIELD_DESC_API_ERROR_DEBUG_MESSAGE).isNull();
        List<ApiSubError> apiSubErrors = apiError.getSubErrors();
        then(apiSubErrors).isNotNull();
        then(apiSubErrors).isNotEmpty();
        then(apiSubErrors.size()).describedAs(FIELD_DESC_API_ERROR_SUB_ERROR_COUNT).isEqualTo(1);
    }

    @Test
    public void validatedObjectConstraintViolationExceptionShouldReturn400AndApiError() {

        TestObjectWithValidation testClass;
        Set<ConstraintViolation<TestObjectWithValidation>> violations;
        LocalValidatorFactoryBean localValidatorFactoryBean = new LocalValidatorFactoryBean();
        localValidatorFactoryBean.afterPropertiesSet();
        Validator localValidator = localValidatorFactoryBean.getValidator();

        testClass = new TestObjectWithValidation(null, "AB");

        violations = localValidator.validate(testClass);

        // given
        ConstraintViolationException constraintViolationException = new ConstraintViolationException(violations);

        // when
        ResponseEntity<ApiError> responseEntity =
                exceptionHandlers.handleConstraintViolationException(constraintViolationException, this.serverHttpRequest);

        then(responseEntity).describedAs("bad request error response").isNotNull();
        then(responseEntity.getStatusCode()).describedAs("bad request error response status").isEqualTo(HttpStatus.BAD_REQUEST);
        then(responseEntity.hasBody()).describedAs(FIELD_DESC_HAS_RESPONSE_BODY).isEqualTo(true);

        ApiError apiError = responseEntity.getBody();
        then(apiError).describedAs(FIELD_DESC_API_ERROR_RESPONSE).isNotNull();
        then(apiError.getStatus()).describedAs(FIELD_DESC_API_ERROR_STATUS).isEqualTo(HttpStatus.BAD_REQUEST.value());
        then(apiError.getTimestamp()).describedAs(FIELD_DESC_API_ERROR_TIMESTAMP).isNotNull();
        then(apiError.getMessage()).describedAs(FIELD_DESC_API_ERROR_MESSAGE).isEqualTo("Validation errors");
        then(apiError.getDebugMessage()).describedAs(FIELD_DESC_API_ERROR_DEBUG_MESSAGE).isNull();

        List<ApiSubError> subErrorList = apiError.getSubErrors();
        then(subErrorList).describedAs(FIELD_DESC_API_ERROR_SUB_ERRORS).isNotNull();
        then(subErrorList.size()).describedAs(FIELD_DESC_API_ERROR_SUB_ERROR_COUNT).isEqualTo(2);

        List<ApiValidationError> validationErrors = subErrorList.stream()
                .filter(subError -> subError instanceof ApiValidationError)
                .map(subError -> (ApiValidationError) subError)
                .sorted(Comparator.comparing(ApiValidationError::getField))
                .collect(Collectors.toList());

        then(subErrorList.size()).describedAs(FIELD_DESC_VALIDATION_ERROR_COUNT).isEqualTo(2);

        ApiValidationError firstValidationError = validationErrors.get(0);
        then(firstValidationError.getField()).describedAs("first validation error field name").isEqualTo("parameter1");
        then(firstValidationError.getRejectedValue()).describedAs("first validation error rejected value").isEqualTo(testClass.parameter1);
        then(firstValidationError.getMessage()).describedAs("first validation error validation message").isNotNull();

        ApiValidationError secondValidationError = validationErrors.get(1);
        then(secondValidationError.getField()).describedAs("second validation error field name").isEqualTo("parameter2");
        then(secondValidationError.getRejectedValue()).describedAs("second validation error rejected value").isEqualTo(testClass.parameter2);
        then(secondValidationError.getMessage()).describedAs("second validation error validation message").isNotNull();

    }

    @Test
    public void validatedMethodConstraintViolationExceptionShouldReturn400AndApiError() throws Exception {
        LocalValidatorFactoryBean localValidatorFactoryBean = new LocalValidatorFactoryBean();
        localValidatorFactoryBean.afterPropertiesSet();
        ExecutableValidator localValidator = localValidatorFactoryBean.getValidator().forExecutables();

        TestObjectWithValidation testClass = new TestObjectWithValidation("ABC", "ABC");
        Method testMethod = TestObjectWithValidation.class.getMethod("testMethodWithValidation", String.class, String.class);
        Object[] parameterValues = {null, "123-12345"};

        Set<ConstraintViolation<TestObjectWithValidation>> violations = localValidator.validateParameters(testClass, testMethod, parameterValues);

        // given
        ConstraintViolationException constraintViolationException = new ConstraintViolationException(violations);

        // when
        ResponseEntity<ApiError> responseEntity =
                exceptionHandlers.handleConstraintViolationException(constraintViolationException, this.serverHttpRequest);

        then(responseEntity).describedAs("bad request error response").isNotNull();
        then(responseEntity.getStatusCode()).describedAs("bad request error response status").isEqualTo(HttpStatus.BAD_REQUEST);
        then(responseEntity.hasBody()).describedAs(FIELD_DESC_HAS_RESPONSE_BODY).isEqualTo(true);

        ApiError apiError = responseEntity.getBody();
        then(apiError).describedAs(FIELD_DESC_API_ERROR_RESPONSE).isNotNull();
        then(apiError.getStatus()).describedAs(FIELD_DESC_API_ERROR_STATUS).isEqualTo(HttpStatus.BAD_REQUEST.value());
        then(apiError.getTimestamp()).describedAs(FIELD_DESC_API_ERROR_TIMESTAMP).isNotNull();
        then(apiError.getMessage()).describedAs(FIELD_DESC_API_ERROR_MESSAGE).isEqualTo("Validation errors");
        then(apiError.getDebugMessage()).describedAs(FIELD_DESC_API_ERROR_DEBUG_MESSAGE).isNull();

        List<ApiSubError> subErrorList = apiError.getSubErrors();
        then(subErrorList).describedAs(FIELD_DESC_API_ERROR_SUB_ERRORS).isNotNull();
        then(subErrorList.size()).describedAs(FIELD_DESC_API_ERROR_SUB_ERROR_COUNT).isEqualTo(2);

        List<ApiValidationError> validationErrors = subErrorList.stream()
                .filter(subError -> subError instanceof ApiValidationError)
                .map(subError -> (ApiValidationError) subError)
                .sorted(Comparator.comparing(ApiValidationError::getField))
                .collect(Collectors.toList());

        then(subErrorList.size()).describedAs("validation error count").isEqualTo(2);

        ApiValidationError firstValidationError = validationErrors.get(0);
        then(firstValidationError.getField()).describedAs("first validation error field name").isEqualTo("parameter1");
        then(firstValidationError.getRejectedValue()).describedAs("first validation error rejected value").isEqualTo(null);
        then(firstValidationError.getMessage()).describedAs("first validation error validation message").isNotNull();

        ApiValidationError secondValidationError = validationErrors.get(1);
        then(secondValidationError.getField()).describedAs("second validation error field name").isEqualTo("parameter2");
        then(secondValidationError.getRejectedValue()).describedAs("second validation error rejected value").isEqualTo("123-12345");
        then(secondValidationError.getMessage()).describedAs("second validation error validation message").isNotNull();

    }

    @Test
    public void missingParameterShouldReturn400AndApiError() {
        // given
        WebExchangeBindException serverRequestBindException = mock(WebExchangeBindException.class);

        // when
        ResponseEntity<ApiError> responseEntity = exceptionHandlers.handleServletRequestBindingException(serverRequestBindException, this.serverHttpRequest);

        then(responseEntity).describedAs("missing header parameter error response").isNotNull();
        then(responseEntity.getStatusCode()).describedAs("missing header parameter response status").isEqualTo(HttpStatus.BAD_REQUEST);
        then(responseEntity.getHeaders().getContentType()).describedAs(FIELD_DESC_CONTENT_TYPE).isEqualTo(MediaType.APPLICATION_JSON);
        then(responseEntity.hasBody()).describedAs(FIELD_DESC_HAS_RESPONSE_BODY).isEqualTo(true);

        ApiError apiError = responseEntity.getBody();
        then(apiError).describedAs(FIELD_DESC_API_ERROR_RESPONSE).isNotNull();
        then(apiError.getStatus()).describedAs(FIELD_DESC_API_ERROR_STATUS).isEqualTo(HttpStatus.BAD_REQUEST.value());
        then(apiError.getTimestamp()).describedAs(FIELD_DESC_API_ERROR_TIMESTAMP).isNotNull();
        then(apiError.getMessage()).describedAs(FIELD_DESC_API_ERROR_MESSAGE).isEqualTo("Validation errors");
        then(apiError.getDebugMessage()).describedAs(FIELD_DESC_API_ERROR_DEBUG_MESSAGE).isNull();

    }

    /**
     * Simple test class with some properties that are annotated with validation constraints
     */
    public static class TestObjectWithValidation {

        @NotNull
        private String parameter1;

        @Min(value = 3)
        private String parameter2;

        TestObjectWithValidation(@NotNull String parameter1, @Min(value = 3) String parameter2) {
            this.parameter1 = parameter1;
            this.parameter2 = parameter2;
        }

        public void testMethodWithValidation(@NotNull String parameter1, @Pattern(regexp = "[a-zA-Z0-9]{9}") String parameter2) {

        }

    }

    @Test
    public void handleServerWebInputExceptionDueToInvalidFormat() {

        InvalidFormatException invalidFormatException = mock(InvalidFormatException.class);
        List<JsonMappingException.Reference> references = new ArrayList<>();
        references.add(new JsonMappingException.Reference("dummyObject", "inputObject"));
        references.add(new JsonMappingException.Reference("dummyDate", "inputDate"));
        when(invalidFormatException.getPath()).thenReturn(references);
        when(invalidFormatException.getValue()).thenReturn("2019-99-999");
        ServerWebInputException serverWebInputException = new ServerWebInputException("some issue", null,
                new DecodingException("some issue", invalidFormatException));

        ResponseEntity<ApiError> responseEntity = exceptionHandlers.handleServerWebInputException(serverWebInputException, this.serverHttpRequest);
        then(responseEntity).describedAs("response for bad request").isNotNull();
        then(responseEntity.getStatusCode()).describedAs("response status for bad request").isEqualTo(HttpStatus.BAD_REQUEST);

        ApiError apiError = responseEntity.getBody();

        then(apiError).describedAs(FIELD_DESC_API_ERROR_RESPONSE).isNotNull();
        then(apiError.getStatus()).describedAs(FIELD_DESC_API_ERROR_STATUS).isEqualTo(HttpStatus.BAD_REQUEST.value());
        then(apiError.getTimestamp()).describedAs(FIELD_DESC_API_ERROR_TIMESTAMP).isNotNull();
        then(apiError.getMessage()).describedAs(FIELD_DESC_API_ERROR_MESSAGE).isEqualTo("Validation errors");

        List<ApiSubError> subErrorList = apiError.getSubErrors();
        then(subErrorList).describedAs(FIELD_DESC_API_ERROR_SUB_ERRORS).isNotNull();
        then(subErrorList.size()).describedAs(FIELD_DESC_API_ERROR_SUB_ERROR_COUNT).isEqualTo(1);

        List<ApiValidationError> validationErrors = subErrorList.stream()
                .filter(subError -> subError instanceof ApiValidationError)
                .map(subError -> (ApiValidationError) subError)
                .sorted(Comparator.comparing(ApiValidationError::getField))
                .collect(Collectors.toList());

        ApiValidationError validationError = validationErrors.get(0);
        then(validationError.getMessage()).describedAs("validation error message").isNotNull();
        then(validationError.getMessage()).describedAs("validation error message").isEqualTo("Invalid format");
        then(validationError.getField()).describedAs("validation field").isEqualTo("inputObject.inputDate");
        then(validationError.getRejectedValue()).describedAs("rejected value").isEqualTo("2019-99-999");
    }

    @Test
    public void testHandleResourceNotFoundException() {

        // given
        ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException("Shipment data not found for...");

        // when
        ResponseEntity<ApiError> responseEntity = exceptionHandlers.handleResourceNotFoundException(resourceNotFoundException, this.serverHttpRequest);

        then(responseEntity).describedAs("data not found response").isNotNull();
        then(responseEntity.getStatusCode()).describedAs("data not found response status").isEqualTo(HttpStatus.NOT_FOUND);
        then(responseEntity.getHeaders().getContentType()).describedAs(FIELD_DESC_CONTENT_TYPE).isEqualTo(MediaType.APPLICATION_JSON);
        then(responseEntity.hasBody()).describedAs(FIELD_DESC_HAS_RESPONSE_BODY).isEqualTo(true);

        ApiError apiError = responseEntity.getBody();
        then(apiError).describedAs(FIELD_DESC_API_ERROR_RESPONSE).isNotNull();
        then(apiError.getStatus()).describedAs(FIELD_DESC_API_ERROR_STATUS).isEqualTo(HttpStatus.NOT_FOUND.value());
        then(apiError.getTimestamp()).describedAs(FIELD_DESC_API_ERROR_TIMESTAMP).isNotNull();
        then(apiError.getMessage()).describedAs(FIELD_DESC_API_ERROR_MESSAGE).isEqualTo("Resource not found");
        then(apiError.getDebugMessage()).describedAs(FIELD_DESC_API_ERROR_DEBUG_MESSAGE).isEqualTo(resourceNotFoundException.getLocalizedMessage());
        then(apiError.getSubErrors()).describedAs(FIELD_DESC_API_ERROR_SUB_ERRORS).isNull();

    }

    @Test
    public void testHandleDuplicateDataFoundException() {

        // given
        DuplicateDataFoundException duplicateDataFoundException = new DuplicateDataFoundException("Duplicate data found for .....");

        // when
        ResponseEntity<ApiError> responseEntity = exceptionHandlers.handleDuplicateDataFoundException(duplicateDataFoundException, this.serverHttpRequest);

        then(responseEntity).describedAs("Duplicate Data Found").isNotNull();
        then(responseEntity.getStatusCode()).describedAs("data not found response status").isEqualTo(HttpStatus.CONFLICT);
        then(responseEntity.getHeaders().getContentType()).describedAs(FIELD_DESC_CONTENT_TYPE).isEqualTo(MediaType.APPLICATION_JSON);
        then(responseEntity.hasBody()).describedAs(FIELD_DESC_HAS_RESPONSE_BODY).isEqualTo(true);

        ApiError apiError = responseEntity.getBody();
        then(apiError).describedAs(FIELD_DESC_API_ERROR_RESPONSE).isNotNull();
        then(apiError.getStatus()).describedAs(FIELD_DESC_API_ERROR_STATUS).isEqualTo(HttpStatus.CONFLICT.value());
        then(apiError.getTimestamp()).describedAs(FIELD_DESC_API_ERROR_TIMESTAMP).isNotNull();
        then(apiError.getMessage()).describedAs(FIELD_DESC_API_ERROR_MESSAGE).isEqualTo("Duplicate Data Found");
        then(apiError.getDebugMessage()).describedAs(FIELD_DESC_API_ERROR_DEBUG_MESSAGE).isEqualTo(duplicateDataFoundException.getLocalizedMessage());
        then(apiError.getSubErrors()).describedAs(FIELD_DESC_API_ERROR_SUB_ERRORS).isNull();

    }

    @Test
    public void handleMethodArgumentNotValidExceptionShouldReturn400AndApiError1() {
        // given
        MethodParameter methodParameter = mock(MethodParameter.class);
        when(methodParameter.getParameterIndex()).thenReturn(0);
        Executable executable = mock(Executable.class);
        when(executable.toGenericString()).thenReturn("Some Executable");
        when(methodParameter.getExecutable()).thenReturn(executable);
        BindingResult bindingResult = mock(BindingResult.class);
        MethodArgumentNotValidException methodArgumentNotValidException = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // when
        ResponseEntity<ApiError> responseEntity = exceptionHandlers.handleMethodArgumentNotValidException(methodArgumentNotValidException, this.serverHttpRequest);


        // then
        then(responseEntity).isNotNull();
        then(responseEntity.getStatusCode()).describedAs("bad request response status").isEqualTo(HttpStatus.BAD_REQUEST);

        FieldError fieldError1 = new FieldError("Code", "CarrierCode", "Invalid Carrier Code");
        FieldError fieldError2 = new FieldError("Code", "CarrierCode", "Invalid Carrier Code");
        bindingResult = new BeanPropertyBindingResult("User", "ID");
        bindingResult.addError(fieldError1);
        bindingResult.addError(fieldError2);
        methodArgumentNotValidException = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // when
        responseEntity = exceptionHandlers.handleBindException(methodArgumentNotValidException, this.serverHttpRequest);

        // then
        then(responseEntity).isNotNull();
        ApiError apiError =  responseEntity.getBody();
        then(apiError.getSubErrors()).isNotNull().isNotEmpty();
        then(apiError.getSubErrors().size()).isEqualTo(2);

        List<ApiValidationError> validationErrors = apiError.getSubErrors().stream()
                .filter(subError -> subError instanceof ApiValidationError)
                .map(subError -> (ApiValidationError) subError)
                .sorted(Comparator.comparing(ApiValidationError::getField))
                .collect(Collectors.toList());
        ApiValidationError firstValidationError = validationErrors.get(0);
        then(firstValidationError.getField()).describedAs("first validation error field name").isEqualTo("CarrierCode");
        then(firstValidationError.getRejectedValue()).describedAs("first validation error rejected value").isNull();
        then(firstValidationError.getMessage()).describedAs("first validation error validation message").isEqualTo("Invalid Carrier Code");

    }
    @Test
    public void testHandleServerWebInputException() {
        // given
        ServerWebInputException serverWebInputException = new ServerWebInputException("reason");

        // when
        ResponseEntity<ApiError> responseEntity = exceptionHandlers.handleServerWebInputException(serverWebInputException, serverHttpRequest);

        then(responseEntity).isNotNull();
        then(responseEntity.getStatusCode()).describedAs("bad request response status").isEqualTo(HttpStatus.BAD_REQUEST);
        then(responseEntity.getHeaders().getContentType()).describedAs(FIELD_DESC_CONTENT_TYPE).isEqualTo(MediaType.APPLICATION_JSON);
        then(responseEntity.hasBody()).describedAs(FIELD_DESC_HAS_RESPONSE_BODY).isEqualTo(true);

        ApiError apiError = responseEntity.getBody();
        then(apiError).describedAs(FIELD_DESC_API_ERROR_RESPONSE).isNotNull();
        then(apiError.getStatus()).describedAs(FIELD_DESC_API_ERROR_STATUS).isEqualTo(HttpStatus.BAD_REQUEST.value());
        then(apiError.getTimestamp()).describedAs(FIELD_DESC_API_ERROR_TIMESTAMP).isNotNull();
        then(apiError.getMessage()).describedAs(FIELD_DESC_API_ERROR_MESSAGE).isEqualTo("Validation errors");
        then(apiError.getDebugMessage()).describedAs(FIELD_DESC_API_ERROR_DEBUG_MESSAGE).isEqualTo(serverWebInputException.getLocalizedMessage());
        then(apiError.getSubErrors()).describedAs(FIELD_DESC_API_ERROR_SUB_ERRORS).isNull();

    }

    @Test
    public void testHandleServerWebInputExceptionCauseDecodingException() {
        // given

        DecodingException decodingException = mock(DecodingException.class);
        List<JsonMappingException.Reference> references = new ArrayList<>();
        references.add(new JsonMappingException.Reference("dummyObject", "inputObject"));
        references.add(new JsonMappingException.Reference("dummyDate", "inputDate"));


        ServerWebInputException serverWebInputException = new ServerWebInputException("some issue", null, decodingException);

        // when
        ResponseEntity<ApiError> responseEntity = exceptionHandlers.handleServerWebInputException(serverWebInputException, serverHttpRequest);

        then(responseEntity).describedAs("response for bad request").isNotNull();
        then(responseEntity.getStatusCode()).describedAs("response status for bad request").isEqualTo(HttpStatus.BAD_REQUEST);

        ApiError apiError = responseEntity.getBody();

        then(apiError).describedAs(FIELD_DESC_API_ERROR_RESPONSE).isNotNull();
        then(apiError.getStatus()).describedAs(FIELD_DESC_API_ERROR_STATUS).isEqualTo(HttpStatus.BAD_REQUEST.value());
        then(apiError.getTimestamp()).describedAs(FIELD_DESC_API_ERROR_TIMESTAMP).isNotNull();
        then(apiError.getMessage()).describedAs(FIELD_DESC_API_ERROR_MESSAGE).isEqualTo("Validation errors");
        then(apiError.getSubErrors()).isNull();

    }
    @Test
    public void handleServerWebInputExceptionDueToMismatchedInputException() {

        MismatchedInputException mismatchedInputException = mock(InvalidFormatException.class);
        List<JsonMappingException.Reference> references = new ArrayList<>();
        references.add(new JsonMappingException.Reference("dummyObject", "inputObject"));
        references.add(new JsonMappingException.Reference("dummyDate", "inputDate"));
        when(mismatchedInputException.getPath()).thenReturn(references);
        ServerWebInputException serverWebInputException = new ServerWebInputException("\\\"Failed to read HTTP message\\\"; nested exception is org.springframework.core.codec.DecodingException: JSON decoding error: Cannot deserialize value of type `java.lang.Boolean` from String \\\"truefff\\\": only \\\"true\\\" or \\\"false\\\" recognized", null,
                new DecodingException("Cannot deserialize value of type `java.lang.Boolean` from String \"truefff\": only \"true\" or \"false\" recognized", mismatchedInputException));

        ResponseEntity<ApiError> responseEntity = exceptionHandlers.handleServerWebInputException(serverWebInputException, this.serverHttpRequest);
        then(responseEntity).describedAs("response for bad request").isNotNull();
        then(responseEntity.getStatusCode()).describedAs("response status for bad request").isEqualTo(HttpStatus.BAD_REQUEST);

        ApiError apiError = responseEntity.getBody();

        then(apiError).describedAs(FIELD_DESC_API_ERROR_RESPONSE).isNotNull();
        then(apiError.getStatus()).describedAs(FIELD_DESC_API_ERROR_STATUS).isEqualTo(HttpStatus.BAD_REQUEST.value());
        then(apiError.getTimestamp()).describedAs(FIELD_DESC_API_ERROR_TIMESTAMP).isNotNull();
        then(apiError.getMessage()).describedAs(FIELD_DESC_API_ERROR_MESSAGE).isEqualTo("Validation errors");

        List<ApiSubError> subErrorList = apiError.getSubErrors();
        then(subErrorList).describedAs(FIELD_DESC_API_ERROR_SUB_ERRORS).isNotNull();
        then(subErrorList.size()).describedAs(FIELD_DESC_API_ERROR_SUB_ERROR_COUNT).isEqualTo(1);

        List<ApiValidationError> validationErrors = subErrorList.stream()
                .filter(subError -> subError instanceof ApiValidationError)
                .map(subError -> (ApiValidationError) subError)
                .sorted(Comparator.comparing(ApiValidationError::getField))
                .collect(Collectors.toList());

        ApiValidationError validationError = validationErrors.get(0);
        then(validationError.getMessage()).describedAs("validation error message").isNotNull();
        then(validationError.getMessage()).describedAs("validation error message").isEqualTo("inputObject.inputDate should be true or false");
        then(validationError.getField()).describedAs("validation field").isEqualTo("inputObject.inputDate");
    }

    @Test
    public void missingParameterShouldReturn400AndApiErrorwithsuberrors() {

        // given
        MethodParameter methodParameter = mock(MethodParameter.class);
        when(methodParameter.getParameterIndex()).thenReturn(0);
        Executable executable = mock(Executable.class);
        when(executable.toGenericString()).thenReturn("Some Executable");
        when(methodParameter.getExecutable()).thenReturn(executable);
        BindingResult bindingResult = mock(BindingResult.class);
        WebExchangeBindException serverRequestBindException = new WebExchangeBindException(methodParameter,bindingResult);

        // when
        ResponseEntity<ApiError> responseEntity = exceptionHandlers.handleServletRequestBindingException(serverRequestBindException, this.serverHttpRequest);

        then(responseEntity).isNotNull();
        then(responseEntity.getStatusCode()).describedAs("bad request response status").isEqualTo(HttpStatus.BAD_REQUEST);

        FieldError fieldError1 = new FieldError("Code", "CarrierCode", "Invalid Carrier Code");
        FieldError fieldError2 = new FieldError("Code", "CarrierCode", "Invalid Carrier Code");
        bindingResult = new BeanPropertyBindingResult("User", "ID");
        bindingResult.addError(fieldError1);
        bindingResult.addError(fieldError2);
        serverRequestBindException = new WebExchangeBindException(methodParameter, bindingResult);

        // when
        responseEntity = exceptionHandlers.handleServletRequestBindingException(serverRequestBindException, this.serverHttpRequest);

        // then
        then(responseEntity).isNotNull();
        ApiError apiError =  responseEntity.getBody();
        then(apiError.getSubErrors()).isNotNull().isNotEmpty();
        then(apiError.getSubErrors().size()).isEqualTo(2);

        List<ApiValidationError> validationErrors = apiError.getSubErrors().stream()
                .filter(subError -> subError instanceof ApiValidationError)
                .map(subError -> (ApiValidationError) subError)
                .sorted(Comparator.comparing(ApiValidationError::getField))
                .collect(Collectors.toList());
        ApiValidationError firstValidationError = validationErrors.get(0);
        then(firstValidationError.getField()).describedAs("first validation error field name").isEqualTo("CarrierCode");
        then(firstValidationError.getRejectedValue()).describedAs("first validation error rejected value").isEqualTo("");
        then(firstValidationError.getMessage()).describedAs("first validation error validation message").isEqualTo("Invalid Carrier Code");


    }

}
