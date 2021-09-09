package net.apmoller.crb.ohp.microservices.exception;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InternalServerExceptionTest {

    @Test
    public void constructorWithException() {
        InternalServerException internalServerException = new InternalServerException("error");
        assertEquals("error", internalServerException.getLocalizedMessage(), "Internal Server exception");
    }
}
