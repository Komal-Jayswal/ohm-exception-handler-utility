package net.apmoller.crb.ohp.microservices.model;

import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;

public class ApiErrorTest {

    private static final String REQUEST_URI = "/path/to/resource";
    private static final String FIELD_DESC_HTTP_METHOD = "http method";
    private static final String FIELD_DESC_REQUEST_URI = "request uri";
    private static final String FIELD_DESC_ERROR_TIMESTAMP = "error timestamp";
    private static final String FIELD_DESC_HTTP_STATUS_CODE = "http status code";
    private static final String FIELD_DESC_ERROR_MESSAGE = "error message";
    private static final String FIELD_DESC_DEBUG_ERROR_MESSAGE = "debug error message";
    private static final String FIELD_DESC_SUB_ERRORS = "sub-errors";
    private static final String FIELD_DESC_ERROR_ID = "error id";

    @Test
    public void createInstanceWithJustHttpStatus() {

        // when
        ApiError apiError = new ApiError(HttpMethod.GET, REQUEST_URI, HttpStatus.NOT_FOUND);

        then(apiError.getMethod()).describedAs(FIELD_DESC_HTTP_METHOD).isEqualTo(HttpMethod.GET);
        then(apiError.getRequestUri()).describedAs(FIELD_DESC_REQUEST_URI).isEqualTo(REQUEST_URI);
        then(apiError.getTimestamp()).describedAs(FIELD_DESC_ERROR_TIMESTAMP).isNotNull();
        then(apiError.getStatus()).describedAs(FIELD_DESC_HTTP_STATUS_CODE).isEqualTo(HttpStatus.NOT_FOUND.value());
        then(apiError.getMessage()).describedAs(FIELD_DESC_ERROR_MESSAGE).isEqualTo(null);
        then(apiError.getDebugMessage()).describedAs(FIELD_DESC_DEBUG_ERROR_MESSAGE).isEqualTo(null);
        then(apiError.getSubErrors()).describedAs(FIELD_DESC_SUB_ERRORS).isNull();
        then(apiError.getId()).describedAs(FIELD_DESC_ERROR_ID).isNotNull();

    }

    @Test
    public void createInstanceForUnknownError() {

        // given
        Exception exception = new Exception("Some error");

        // when
        ApiError apiError = new ApiError(HttpMethod.GET, REQUEST_URI, HttpStatus.NOT_FOUND, exception);

        then(apiError.getMethod()).describedAs(FIELD_DESC_HTTP_METHOD).isEqualTo(HttpMethod.GET);
        then(apiError.getRequestUri()).describedAs(FIELD_DESC_REQUEST_URI).isEqualTo(REQUEST_URI);
        then(apiError.getTimestamp()).describedAs(FIELD_DESC_ERROR_TIMESTAMP).isNotNull();
        then(apiError.getStatus()).describedAs(FIELD_DESC_HTTP_STATUS_CODE).isEqualTo(HttpStatus.NOT_FOUND.value());
        then(apiError.getMessage()).describedAs(FIELD_DESC_ERROR_MESSAGE).isEqualTo("Unexpected error");
        then(apiError.getDebugMessage()).describedAs(FIELD_DESC_DEBUG_ERROR_MESSAGE).isEqualTo(exception.getLocalizedMessage());
        then(apiError.getSubErrors()).describedAs(FIELD_DESC_SUB_ERRORS).isNull();
        then(apiError.getId()).describedAs(FIELD_DESC_ERROR_ID).isNotNull();

    }

    @Test
    public void createInstanceForExpectedError() {

        // given
        Exception exception = new Exception("Some error");

        // when
        ApiError apiError = new ApiError(HttpMethod.GET, REQUEST_URI, HttpStatus.NOT_FOUND, "Data not found", exception);

        then(apiError.getMethod()).describedAs(FIELD_DESC_HTTP_METHOD).isEqualTo(HttpMethod.GET);
        then(apiError.getRequestUri()).describedAs(FIELD_DESC_REQUEST_URI).isEqualTo(REQUEST_URI);
        then(apiError.getTimestamp()).describedAs(FIELD_DESC_ERROR_TIMESTAMP).isNotNull();
        then(apiError.getStatus()).describedAs(FIELD_DESC_HTTP_STATUS_CODE).isEqualTo(HttpStatus.NOT_FOUND.value());
        then(apiError.getMessage()).describedAs(FIELD_DESC_ERROR_MESSAGE).isEqualTo("Data not found");
        then(apiError.getDebugMessage()).describedAs(FIELD_DESC_DEBUG_ERROR_MESSAGE).isEqualTo(exception.getLocalizedMessage());
        then(apiError.getSubErrors()).describedAs(FIELD_DESC_SUB_ERRORS).isNull();
        then(apiError.getId()).describedAs(FIELD_DESC_ERROR_ID).isNotNull();

    }

    @Test
    public void instanceWithSubErrors() {

        // given
        List<ApiSubError> subErrors = Arrays.asList(
                new ApiValidationError("Field1", "Rejected Value1", "Validation error message1"),
                new ApiValidationError("Field2", "Rejected Value2", "Validation error message2"));

        // when
        ApiError apiError = new ApiError(HttpMethod.GET, REQUEST_URI, HttpStatus.NOT_FOUND);
        apiError.setSubErrors(subErrors);

        then(apiError.getSubErrors().size()).describedAs("number of sub-errors").isEqualTo(2);

    }
}
