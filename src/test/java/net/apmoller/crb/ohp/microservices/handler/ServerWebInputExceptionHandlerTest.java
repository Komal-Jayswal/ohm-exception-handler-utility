package net.apmoller.crb.ohp.microservices.handler;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import net.apmoller.crb.ohp.microservices.model.ApiError;
import net.apmoller.crb.ohp.microservices.model.ApiSubError;
import net.apmoller.crb.ohp.microservices.model.ApiValidationError;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.server.ServerWebInputException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;

public class ServerWebInputExceptionHandlerTest {

    private ServerWebInputExceptionHandler serverWebInputExceptionHandlerUnderTest = new ServerWebInputExceptionHandler();
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

    @Before
    public void setUp() {
        this.serverHttpRequest = MockServerHttpRequest
                .method(HttpMethod.POST, "aUrl")
                .build();
    }

    @Test
    public void testHandleServerWebInputException() {
        // given
        ServerWebInputException serverWebInputException = new ServerWebInputException("reason");

        // when
        ResponseEntity<ApiError> responseEntity = serverWebInputExceptionHandlerUnderTest.handleServerWebInputException(serverWebInputException, serverHttpRequest);

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
    public void testHandleServerWebInputExceptionCauseInvalidFormatException() {
        // given

        InvalidFormatException invalidFormatException = mock(InvalidFormatException.class);
        List<JsonMappingException.Reference> references = new ArrayList<>();
        references.add(new JsonMappingException.Reference("dummyObject", "inputObject"));
        references.add(new JsonMappingException.Reference("dummyDate", "inputDate"));
        Mockito.when(invalidFormatException.getPath()).thenReturn(references);
        Mockito.when(invalidFormatException.getValue()).thenReturn("2019-99-999");

        ServerWebInputException serverWebInputException = new ServerWebInputException("some issue", null, new DecodingException("some issue", invalidFormatException));

        // when
        ResponseEntity<ApiError> responseEntity = serverWebInputExceptionHandlerUnderTest.handleServerWebInputException(serverWebInputException, serverHttpRequest);

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
    public void testHandleServerWebInputExceptionInstanceOfValueInstantiationException() {
        // given
        ValueInstantiationException valueInstantiationException = mock(ValueInstantiationException.class);

        List<JsonMappingException.Reference> references = new ArrayList<>();
        references.add(new JsonMappingException.Reference("dummyObject", "inputObject"));
        references.add(new JsonMappingException.Reference("dummyDate", "inputDate"));
        Mockito.when(valueInstantiationException.getPath()).thenReturn(references);


        ServerWebInputException serverWebInputException = new ServerWebInputException("some issue", null, new DecodingException("some issue", valueInstantiationException));

        // when
        ResponseEntity<ApiError> responseEntity = serverWebInputExceptionHandlerUnderTest.handleServerWebInputException(serverWebInputException, serverHttpRequest);

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
        then(validationError.getMessage()).describedAs("validation error message").isEqualTo("Invalid value");
        then(validationError.getField()).describedAs("validation field").isEqualTo("inputObject.inputDate");
        then(validationError.getRejectedValue()).describedAs("rejected value").isNull();


    }
}
