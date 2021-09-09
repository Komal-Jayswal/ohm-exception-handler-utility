package net.apmoller.crb.ohp.microservices.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import net.apmoller.crb.ohp.microservices.model.ApiError;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doReturn;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ExceptionUtils.class)
@PowerMockIgnore("javax.management.*")
public class ExceptionUtilsTest {

    private static final String REQUEST_URI = "/path/to/resource";

    @Test
    public void givenApiErrorForUnknownExceptionShouldGenerateLogDebugMessageString() {

        // given
        Exception exception = new NullPointerException("Unexpected error");

        // when
        String stackTraceDetails = ExceptionUtils.generateApiStackTraceString(exception, "someTraceId");

        // then
        then(stackTraceDetails).describedAs("stack trace details string").isNotBlank();
        then(stackTraceDetails).describedAs("stack trace details string contains").contains(exception.toString());

    }

    @Test
    public void givenApiErrorForUnknownExceptionShouldIncludeStackTraceElementForMaerskClassWhenWithinSearchLimit() {

        // As we only get a limited number of stack trace elements returned, if the stack trace elements selected don't
        // originate from 'Maersk' class i.e. net.apmoller package prefix then additional stack trace elements are checked
        // (there is a limit as to how many additional elements will be checked) for net.apmoller package and appended to
        // the list if found.

        // given
        Exception exception = new Exception("Unexpected error");

        StackTraceElement[] stackTrace = populateDummyStackTrace(21, 20);
        stackTrace[20] = new StackTraceElement("net.apmoller.some.class", "methodName", "fileName", 999);
        exception.setStackTrace(stackTrace);

        // when
        String stackTraceDetails = ExceptionUtils.generateApiStackTraceString(exception, "someTraceId");

        // then
        then(stackTraceDetails).describedAs("stack trace details string").isNotBlank();
        then(stackTraceDetails).describedAs("stack trace details string contains").contains("net.apmoller.some.class");

    }

    @Test
    public void givenApiErrorForUnknownExceptionShouldNotIncludeStackTraceElementForMaerskClassWhenOutsideSearchLimit() {

        // As we only get a limited number of stack trace elements returned, if the stack trace elements selected don't
        // originate from 'Maersk' class i.e. net.apmoller package prefix then additional stack trace elements are checked
        // (there is a limit as to how many additional elements will be checked) for net.apmoller package and appended to
        // the list if found. In this instance 'net.apmoller' stack trace element occurs after the max search limit so
        // we don't expect it to be returned.

        // given
        Exception exception = new Exception("Unexpected error");

        StackTraceElement[] stackTrace = populateDummyStackTrace(101, 100);
        stackTrace[100] = new StackTraceElement("net.apmoller.some.class", "methodName", "fileName", 999);
        exception.setStackTrace(stackTrace);

        // when
        String stackTraceDetails = ExceptionUtils.generateApiStackTraceString(exception, "someTraceId");

        // then
        then(stackTraceDetails).describedAs("stack trace details string").isNotBlank();
        then(stackTraceDetails).describedAs("stack trace details string contains").doesNotContain("net.apmoller.some.class");

    }

    @Test
    public void givenApiErrorForUnknownExceptionAndJsonExceptionShouldStillGenerateLogErrorMessageString() throws Exception {

        // given
        Exception exception = new NullPointerException("Unexpected error");

        // when
        ObjectWriter mockObjectWriter = mock(ObjectWriter.class);
        PowerMockito.spy(ExceptionUtils.class);
        doReturn(mockObjectWriter).when(ExceptionUtils.class, "getObjectWriter");
        when(mockObjectWriter.writeValueAsString(any(Object.class))).thenThrow(new JsonProcessingException("") {
        });

        String stackTraceDetails = ExceptionUtils.generateApiStackTraceString(exception, "someTraceId");

        then(stackTraceDetails).isNotBlank();
        then(stackTraceDetails).contains("exceptionDescription");
        then(stackTraceDetails).contains("lines");
        then(stackTraceDetails).contains("ApiStackTrace");

    }

    @Test
    public void givenApiErrorShouldGenerateLogErrorMessageString() {

        // given
        ApiError apiError = new ApiError(HttpMethod.GET, REQUEST_URI, HttpStatus.NOT_FOUND);

        // when
        String logErrorString = ExceptionUtils.generateApiErrorString(apiError);

        then(logErrorString).isNotBlank();
        then(logErrorString).contains("\"status\":404");

    }

    @Test
    public void givenApiErrorAndJsonExceptionShouldStillGenerateLogErrorMessageString() throws Exception {

        // given
        ApiError apiError = new ApiError(HttpMethod.GET, REQUEST_URI, HttpStatus.NOT_FOUND);

        // when
        ObjectWriter mockObjectWriter = mock(ObjectWriter.class);
        PowerMockito.spy(ExceptionUtils.class);
        doReturn(mockObjectWriter).when(ExceptionUtils.class, "getObjectWriter");
        when(mockObjectWriter.writeValueAsString(any(Object.class))).thenThrow(new JsonProcessingException("") {
        });

        String logErrorString = ExceptionUtils.generateApiErrorString(apiError);

        then(logErrorString).isNotBlank();
        then(logErrorString).contains("errorDetails");
        then(logErrorString).contains("ApiError");

    }

    private StackTraceElement[] populateDummyStackTrace(int stackTraceArraySize, int numberOfElementsToPopulate) {

        StackTraceElement[] stackTrace = new StackTraceElement[stackTraceArraySize];
        for (int i = 0; i < numberOfElementsToPopulate; i++) {
            StackTraceElement stackTraceElement = new StackTraceElement("declaringClass", "methodName",
                    "fileName", i);
            stackTrace[i] = stackTraceElement;
        }
        return stackTrace;
    }

}
