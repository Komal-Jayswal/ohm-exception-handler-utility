package net.apmoller.crb.ohp.microservices.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;

public class ApiStackTraceTest {

    @Test
    public void createInstanceShouldPopulateFieldsCorrectly() {

        // given
        String exceptionDescription = "Exception description";
        List<String> stackTraceLines = Arrays.asList("line1", "line2");

        // when
        ApiStackTrace apiStackTrace = new ApiStackTrace(exceptionDescription, stackTraceLines, "someTraceId");

        // then
        then(apiStackTrace.getExceptionDescription()).describedAs("error description").isEqualTo(exceptionDescription);
        then(apiStackTrace.getLines()).describedAs("stack trace lines ").isNotNull();
        then(apiStackTrace.getLines().size()).describedAs("stack trace lines count").isEqualTo(stackTraceLines.size());

    }

}
