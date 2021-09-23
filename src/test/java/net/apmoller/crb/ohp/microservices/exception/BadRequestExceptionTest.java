package net.apmoller.crb.ohp.microservices.exception;

import net.apmoller.crb.ohp.microservices.model.ApiError;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class BadRequestExceptionTest {

    @Test
    public void constructorWithApiError() {
        ApiError apiError = new ApiError(HttpMethod.GET, "/testEndpoint", HttpStatus.BAD_REQUEST);
        BadRequestException badRequestException = new BadRequestException(apiError);
        assertNotNull("ApiError should not be null", badRequestException.getApiError());
        assertEquals("Invalid error", "/testEndpoint", badRequestException.getApiError().getRequestUri());
    }

    @Test
    public void constructorWithException() {
        BadRequestException badRequestException = new BadRequestException("error");
        assertEquals("error", badRequestException.getLocalizedMessage(), "error");
    }
}