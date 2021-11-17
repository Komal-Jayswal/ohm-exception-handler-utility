package com.example.model;

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
/**
 * Class representing an API error in order to provide details of exceptions thrown back to the client
 */
@Data
@JsonRootName("apiError")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiHTTPErrorResponse extends DefaultErrorAttributes {

    private String id; // UUID of error which can be used for searching logs
    private HttpMethod method;
    private String requestUri;
    private String returnCode;
    private int statusCode;
    private String timestamp;
    private String message;
    private List<ValidationErrorMsg> validationMsg;
    private String debugMessage;
    private List<ApiSubError> subErrors;

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public ApiHTTPErrorResponse() {
        timestamp = dateFormatter.format(LocalDateTime.now(ZoneOffset.UTC));
        id = Optional.ofNullable(MDC.get("traceId")).orElseGet(() -> UUID.randomUUID().toString());
    }

    //validation
    public ApiHTTPErrorResponse(HttpMethod method, String requestUri, HttpStatus statusCode, String returnCode,
                                List<ValidationErrorMsg> eo, Throwable ex) {
        this();
        this.method = method;
        this.requestUri = requestUri;
        this.returnCode = returnCode;
        this.statusCode = statusCode.value();
        this.message = "Validation Error, currently we are putting all validations under 400, later will be modified as per new design";
        this.validationMsg = eo;
        this.debugMessage = ex.getLocalizedMessage();
    }

    //null pointer exception  ,,, //Data Not Found
    public ApiHTTPErrorResponse(HttpMethod method, String requestUri, HttpStatus statusCode, String returnCode, Throwable ex) {
        this();
        this.method = method;
        this.requestUri = requestUri;
        this.returnCode = returnCode;
        this.statusCode = statusCode.value();
        this.message = ex.getMessage();
    }

    public void setSubErrors(List<ApiSubError> subErrors) {
        if (subErrors != null) {
            this.subErrors = new ArrayList<>(subErrors);
        }
    }

    public String getId() {
        return id;
    }

}
