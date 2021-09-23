package net.apmoller.crb.ohp.microservices.exception;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UnauthorizedExceptionTest {

    @Test
    public void constructorWithException() {
        UnauthorizedException unauthorizedException = new UnauthorizedException("error");
        assertEquals("error", unauthorizedException.getLocalizedMessage(), "Unauthorized exception");
    }
}
