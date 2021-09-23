package net.apmoller.crb.ohp.microservices.exception;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ForbiddenExceptionTest {

    @Test
    public void constructorWithException() {
        ForbiddenException forbiddenException = new ForbiddenException("error");
        assertEquals("error", forbiddenException.getLocalizedMessage(), "Forbidden exception");
    }
}
