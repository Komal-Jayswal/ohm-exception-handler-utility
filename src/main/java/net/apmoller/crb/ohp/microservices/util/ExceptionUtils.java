package net.apmoller.crb.ohp.microservices.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.extern.slf4j.Slf4j;
import net.apmoller.crb.ohp.microservices.model.ApiError;
import net.apmoller.crb.ohp.microservices.model.ApiStackTrace;
import net.apmoller.crb.ohp.microservices.model.ApiSubError;
import net.apmoller.crb.ohp.microservices.model.ApiValidationError;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ElementKind;
import javax.validation.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ExceptionUtils {

    public ExceptionUtils() {
    }

    public static final int NUMBER_OF_STACK_TRACE_LINES_TO_PRINT = 15;
    public static final int MAX_STACK_TRACE_LINES_TO_SEARCH_FOR_MAERSK_PACKAGE = 100;
    private static final String MAERSK_PACKAGE_PREFIX = "net.apmoller";
    public static final String DUPLICATE_DATA_FOUND_DESCRIPTION = "Duplicate Data Found";
    public static final String DATA_NOT_FOUND_DESCRIPTION = "Data not found";
    public static final String BAD_REQUEST_DESCRIPTION = "Bad Request";
    public static final String VALIDATION_ERROR_DESCRIPTION = "Validation errors";
    public static final String UNAUTHORIZED_DESCRIPTION = "Unauthorized";
    public static final String FORBIDDEN_DESCRIPTION = "Forbidden";
    public static final String UNEXPECTED_ERROR_DESCRIPTION = "Unexpected error";
    public static final String INTERNAL_SERVER_ERROR_DESCRIPTION = "Internal Server Error";
    public static final String UNSUPPORTED_MEDIA_TYPE_DESCRIPTION = "Unsupported Media Type";
    public static final String RESOURCE_NOT_FOUND_DESCRIPTION = "Resource not found";

    private static ObjectWriter objectWriter;

    private static ObjectWriter getObjectWriter() {
        if (null == objectWriter) {
            ObjectMapper objectMapper = new ObjectMapper();
            objectWriter = objectMapper.writer();
        }
        return objectWriter;
    }

    /**
     * Log error details
     *
     * @param apiError {@link ApiError}
     */
    public static void logError(ApiError apiError) {
        logError(apiError, null);
    }

    /**
     * Log error details
     *
     * @param apiError  {@link ApiError}
     * @param exception Exception for which stack trace details are to be logged, if null then no details will be logged.
     */
    public static void logError(ApiError apiError, Throwable exception) {

        String apiErrorString = generateApiErrorString(apiError);
        log.error("{}", apiErrorString);

        if (null != exception) {
            String apiStackTraceString = generateApiStackTraceString(exception, apiError.getId());
            log.error("{}", apiStackTraceString);
        }

    }

    /**
     * For the passed in exception extracts a subset of stack trace lines (as defined by the
     * NUMBER_OF_STACK_TRACE_LINES_TO_PRINT)
     *
     * @param exception Exception for which to generate stack trace information
     * @param traceId   UUID send back in API error response which can be used to allow searching logs.
     * @return apiStackTraceString
     */
    public static String generateApiStackTraceString(Throwable exception, String traceId) {

        String apiStackTraceString;

        StackTraceElement[] stackTraceElements = exception.getStackTrace();

        int linesToPrint = Math.min(stackTraceElements.length, NUMBER_OF_STACK_TRACE_LINES_TO_PRINT);
        int maxLinesToSearchForMaerskPackagePrint = Math.min(stackTraceElements.length, MAX_STACK_TRACE_LINES_TO_SEARCH_FOR_MAERSK_PACKAGE);

        List<String> stackTraceLines = Arrays.stream(stackTraceElements)
                .limit(linesToPrint)
                .map(StackTraceElement::toString)
                .collect(Collectors.toList());

        if (stackTraceLines.stream().noneMatch(s -> s.contains(MAERSK_PACKAGE_PREFIX))) {
            Arrays.stream(stackTraceElements)
                    .limit(maxLinesToSearchForMaerskPackagePrint)
                    .filter(stackLine -> stackLine.toString().contains(MAERSK_PACKAGE_PREFIX))
                    .findFirst()
                    .ifPresent(stackTraceLine -> stackTraceLines.add(stackTraceLine.toString()));
        }

        ApiStackTrace apiStackTrace = new ApiStackTrace(exception.toString(), stackTraceLines, traceId);

        try {
            apiStackTraceString = getObjectWriter().writeValueAsString(apiStackTrace);
        } catch (JsonProcessingException jsonProcessingException) {
            apiStackTraceString = "{ \"stackTraceDetails\" : \"" + apiStackTrace.toString() + "\" }";
        }

        return apiStackTraceString;
    }

    /**
     * Generate string representation of {@link ApiError} object
     *
     * @param apiError apiErrorString = objectMapper.writeValueAsString(apiError);
     * @return apiErrorString error message to write to log
     */
    public static String generateApiErrorString(ApiError apiError) {

        String apiErrorString;
        try {
            apiErrorString = getObjectWriter().writeValueAsString(apiError);
        } catch (JsonProcessingException jsonProcessingException) {
            apiErrorString = "{ \"errorDetails\" : \"" + apiError.toString() + "\" }";
        }
        return apiErrorString;

    }

    /**
     * Construct validation error object from {@link ConstraintViolationException}
     *
     * @param e                   the {@link ConstraintViolationException}
     * @param validationSubErrors the List of {@link ApiError}
     */
    public static void constructValidationErrorObject(ConstraintViolationException e, List<ApiSubError> validationSubErrors) {
        for (ConstraintViolation constraintViolation : e.getConstraintViolations()) {
            for (Path.Node propertyPath : constraintViolation.getPropertyPath()) {
                // In the case we are validating an object we are interested in the property name, for a validated
                // method call we are interested in the parameter name
                if (propertyPath.getKind() == ElementKind.PROPERTY || propertyPath.getKind() == ElementKind.PARAMETER) {
                    ApiValidationError validationError = new ApiValidationError(propertyPath.getName(), constraintViolation.getInvalidValue(), constraintViolation.getMessage());
                    validationSubErrors.add(validationError);
                    break;
                }
            }
        }
    }
}
