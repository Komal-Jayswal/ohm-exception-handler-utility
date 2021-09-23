package net.apmoller.crb.ohp.microservices.exception;

import net.apmoller.crb.ohp.microservices.model.ApiValidationError;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InvalidEnumExceptionTest {

    @Test
    public void constructorWithException() {
        ApiValidationError apiValidationError= new ApiValidationError("carrierCode","abcd","invalid");
        InvalidEnumException invalidEnumException = new InvalidEnumException(apiValidationError);
        assertEquals("Validation failure", invalidEnumException.getLocalizedMessage(), "Validation failure");
    }
}
