package net.apmoller.crb.ohp.microservices.handler;

import net.apmoller.crb.ohp.microservices.exception.BadRequestException;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;

public class ReactiveErrorHandlerTest {

    ReactiveErrorHandler reactiveErrorHandler = new ReactiveErrorHandler();

    @Test
    public void validateQueryParameterForMandatory() {
        //given
        String parameterValueToValidate = "testingQueryParam";

        // when
        String parameterValue = reactiveErrorHandler.validateQueryParameter(parameterValueToValidate, "queryName", "[a-zA-Z-]{3,50}", true);

        then(parameterValue).isEqualTo(parameterValueToValidate);
    }

    @Test
    public void validateQueryParameterForNonMandatory() {
        //given
        String parameterValueToValidate = "testingQueryParam";

        // when
        String parameterValue = reactiveErrorHandler.validateQueryParameter(parameterValueToValidate, "queryName", "[a-zA-Z-]{3,50}", false);

        then(parameterValue).isEqualTo(parameterValueToValidate);
    }

    @Test(expected = BadRequestException.class)
    public void validateMandatoryQueryParameterIfNotPassedThrowException() {
        // when
        reactiveErrorHandler.validateQueryParameter(null, "queryName", "[a-zA-Z-]{3,50}", true);
    }

    @Test(expected = BadRequestException.class)
    public void validateQueryParameterAndThrowExceptionIfInvalid() {
        // when
        List.of(false, true).forEach(isMandatory -> reactiveErrorHandler.validateQueryParameter("testingQueryParam", "queryName", "[0-9]{3,50}", isMandatory));
    }

}