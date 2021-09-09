package net.apmoller.crb.ohp.microservices.model;

import org.junit.Test;

import static org.assertj.core.api.BDDAssertions.then;

public class ApiValidationErrorTest {

    @Test
    public void createInstanceShouldPopulateAllFields() {

        ApiValidationError validationError = new ApiValidationError("field", "123", "validation message");

        then(validationError.getField()).describedAs("object that failed validation").isEqualTo(validationError.getField());
        then(validationError.getRejectedValue()).describedAs("rejected value").isEqualTo(validationError.getRejectedValue());
        then(validationError.getMessage()).describedAs("validation message").isEqualTo(validationError.getMessage());

    }

}
