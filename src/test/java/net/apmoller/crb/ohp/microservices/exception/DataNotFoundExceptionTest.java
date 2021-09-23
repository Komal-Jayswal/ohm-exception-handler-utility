package net.apmoller.crb.ohp.microservices.exception;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataNotFoundExceptionTest {

    @Test
    public void constructorWithException() {
        DataNotFoundException dataNotFoundExceptionTest = new DataNotFoundException("error");
        assertEquals("error", dataNotFoundExceptionTest.getLocalizedMessage(), "Data not found exception");
    }
}
