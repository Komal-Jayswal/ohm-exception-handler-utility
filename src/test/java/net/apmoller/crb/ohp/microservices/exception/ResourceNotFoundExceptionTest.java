package net.apmoller.crb.ohp.microservices.exception;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ResourceNotFoundExceptionTest {
    @Test
   public void constructorWithException() {
        ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException("error");
        assertEquals("error", resourceNotFoundException.getLocalizedMessage(), "Resource Not Found exception");
    }
}
