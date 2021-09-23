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
