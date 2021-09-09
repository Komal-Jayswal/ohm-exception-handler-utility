package net.apmoller.crb.ohp.microservices.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.Data;
import org.slf4j.MDC;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static net.apmoller.crb.ohp.microservices.util.ExceptionUtils.UNEXPECTED_ERROR_DESCRIPTION;

/**
 * Class representing an API error in order to provide details of exceptions thrown back to the client
 */
@Data
@JsonRootName("apiError")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError extends DefaultErrorAttributes {


    private HttpMethod method;
    private String requestUri;
    private int status;
//    @JsonProperty("timestamp")
//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private String timestamp;
    private String message;
    private String debugMessage;
    private List<net.apmoller.crb.ohp.microservices.model.ApiSubError> subErrors;
    private String id; // UUID of error which can be used for searching logs

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");


    public ApiError() {
        timestamp = dateFormatter.format(LocalDateTime.now(ZoneOffset.UTC));
        id = Optional.ofNullable(MDC.get("traceId")).orElseGet(() -> UUID.randomUUID().toString());
    }

    public ApiError(HttpMethod method, String requestUri, HttpStatus status) {
        this();
        this.method = method;
        this.requestUri = requestUri;
        this.status = status.value();
    }

    public ApiError(HttpMethod method, String requestUri, HttpStatus status, Throwable ex) {
        this();
        this.method = method;
        this.requestUri = requestUri;
        this.status = status.value();
        this.message = UNEXPECTED_ERROR_DESCRIPTION;
        this.debugMessage = ex.getLocalizedMessage();
    }

    public ApiError(HttpMethod method, String requestUri, HttpStatus status, String message, Throwable ex) {
        this();
        this.method = method;
        this.requestUri = requestUri;
        this.status = status.value();
        this.message = message;
        this.debugMessage = ex.getLocalizedMessage();
    }


//    public Map<String, Object> getErrorAttributes(ServerRequest request, boolean includeStackTrace) {
//        Map<String, Object> errorAttributes = new LinkedHashMap<>();
//        Throwable error = this.getError(request);
//        HttpStatus errorStatus = determineApiHttpStatus(error);
//        String message;
//        switch (errorStatus.value()) {
//            case BAD_REQUEST_CODE:
//                message = BAD_REQUEST_DESCRIPTION;
//                break;
//            case UNAUTHORIZED_CODE:
//                message = UNAUTHORIZED_DESCRIPTION;
//                break;
//            case FORBIDDEN_CODE:
//                message = FORBIDDEN_DESCRIPTION;
//                break;
//            case DATA_NOT_FOUND_CODE:
//                message = DATA_NOT_FOUND_DESCRIPTION;
//                break;
//            default:
//                message = UNEXPECTED_ERROR_DESCRIPTION;
//
//        }
//
//        this.method = request.method();
//        this.requestUri = request.path();
//        this.status = errorStatus.value();
//        this.message = message;
//        if (status != HttpStatus.INTERNAL_SERVER_ERROR.value()) {
//            this.debugMessage = determineApiMessage(error);
//        }
//
//        errorAttributes.put("method", method);
//        errorAttributes.put("requestUri", requestUri);
//        errorAttributes.put("status", status);
//        errorAttributes.put("timestamp", timestamp);
//        errorAttributes.put("message", message);
//        errorAttributes.put("debugMessage", debugMessage);
//        errorAttributes.put("subErrors", null);
//        errorAttributes.put("id", id);
//
//     logError(this, determineApiException(error));
//
//        return errorAttributes;
//    }
//
//
//    private Throwable determineApiException(Throwable error) {
//        if (error instanceof ResponseStatusException) {
//            return error.getCause() != null ? error.getCause() : error;
//        } else {
//            return error;
//        }
//    }
//
//    private HttpStatus determineApiHttpStatus(Throwable error) {
//        if (error instanceof ResponseStatusException) {
//            return ((ResponseStatusException) error).getStatus();
//        }
//        ResponseStatus responseStatus = AnnotatedElementUtils
//                .findMergedAnnotation(error.getClass(), ResponseStatus.class);
//        if (responseStatus != null) {
//            return responseStatus.code();
//        }
//        return HttpStatus.INTERNAL_SERVER_ERROR;
//    }
//
//    private String determineApiMessage(Throwable error) {
//        if (error instanceof WebExchangeBindException) {
//            return error.getMessage();
//        }
//        if (error instanceof ResponseStatusException) {
//            return ((ResponseStatusException) error).getReason();
//        }
//        if (error instanceof DataNotFoundException) {
//            return error.getMessage();
//        }
//        ResponseStatus responseStatus = AnnotatedElementUtils
//                .findMergedAnnotation(error.getClass(), ResponseStatus.class);
//        if (responseStatus != null) {
//            return responseStatus.reason();
//        }
//        return error.getMessage();
//    }
    public List<net.apmoller.crb.ohp.microservices.model.ApiSubError> getSubErrors() {
        if (subErrors == null) {
            return null;
        }
        return new ArrayList<>(subErrors);
    }

    public void setSubErrors(List<net.apmoller.crb.ohp.microservices.model.ApiSubError> subErrors) {
        if (subErrors != null) {
            this.subErrors = new ArrayList<>(subErrors);
        }
    }

    public String getId() {
        return id;
    }

}
