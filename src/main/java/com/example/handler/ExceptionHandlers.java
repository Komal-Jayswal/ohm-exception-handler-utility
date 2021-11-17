package com.example.handler;

import com.example.exception.NullPointerException;
import com.example.model.ApiSubError;
import com.example.model.ValidationErrorMsg;
import lombok.extern.slf4j.Slf4j;
import com.example.exception.*;
import com.example.model.ApiHTTPErrorResponse;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.List;
/**
 * Set of generic error handlers to be used by Spring Boot RestController classes.
 */
@RestControllerAdvice
@Slf4j
public class ExceptionHandlers extends ResponseEntityExceptionHandler{

    @Override
    public ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException methodArgumentNotValidException,
            HttpHeaders headers, HttpStatus status, WebRequest request) {

        ApiHTTPErrorResponse apiHTTPErrorResponse = new ApiHTTPErrorResponse(((ServletWebRequest)request).getHttpMethod(),
                ((ServletWebRequest)request).getRequest().getServletPath() ,
        		HttpStatus.BAD_REQUEST, "1", methodArgumentNotValidException);

        List<ApiSubError> validationSubErrors = new ArrayList<>();
        for (FieldError fieldError : methodArgumentNotValidException.getBindingResult().getFieldErrors()) {

            ApiSubError validationError = new ApiSubError(fieldError.getField(),
                    fieldError.getRejectedValue(), fieldError.getDefaultMessage());
            validationSubErrors.add(validationError);
        }
        if(!validationSubErrors.isEmpty()){
            apiHTTPErrorResponse.setSubErrors(validationSubErrors);
        }
        return new ResponseEntity(apiHTTPErrorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Build the response entity to be returned.
     * @param apiHTTPErrorResponse API error object {@link ApiHTTPErrorResponse}
     * @return responseEntity
     */
    private ResponseEntity<ApiHTTPErrorResponse> buildResponseEntity(ApiHTTPErrorResponse apiHTTPErrorResponse) {

        return ResponseEntity
                .status(apiHTTPErrorResponse.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(apiHTTPErrorResponse);
    }

    /**
     * Data not found exception handler.
     * @param validationError {@link ValidationError}
     * @return responseEntity with status set to 404 and payload set to {@link ApiHTTPErrorResponse}
     */
    @ExceptionHandler(value = ValidationError.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiHTTPErrorResponse> handleDataNotFoundException(ValidationError validationError,
                                                                            ServletWebRequest servletWebRequest) {

        List<ValidationErrorMsg> validationErrorMsgList = new ArrayList<>(validationError.getValidationErrorMsg());

        ApiHTTPErrorResponse apiHTTPErrorResponse = new ApiHTTPErrorResponse(servletWebRequest.getHttpMethod(),
                servletWebRequest.getRequest().getServletPath(),
                HttpStatus.BAD_REQUEST,
                validationError.getReturnCode(),
                validationErrorMsgList,
                validationError);

        return buildResponseEntity(apiHTTPErrorResponse);
    }

    /**
     * Internal server error exception handler.
     * @param nullPointerException {@link NullPointerException}
     * @return responseEntity with status set to 500 and payload set to {@link ApiHTTPErrorResponse}
     */
    @ExceptionHandler(value = NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiHTTPErrorResponse> handleNullPointerException(NullPointerException nullPointerException,
                                                                              ServletWebRequest servletWebRequest) {

        ApiHTTPErrorResponse apiHTTPErrorResponse = new ApiHTTPErrorResponse(servletWebRequest.getHttpMethod(), servletWebRequest.getRequest().getServletPath(),
                HttpStatus.INTERNAL_SERVER_ERROR, nullPointerException.getReturnCode(),nullPointerException);

        return buildResponseEntity(apiHTTPErrorResponse);
    }

    /**
     * Data not found exception handler.
     * @param dataNotFoundException {@link DataNotFoundException}
     * @return responseEntity with status set to 404 and payload set to {@link ApiHTTPErrorResponse}
     */
    @ExceptionHandler(value = DataNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ApiHTTPErrorResponse> handleDataNotFoundException(DataNotFoundException dataNotFoundException,
                                                                ServletWebRequest servletWebRequest) {

        ApiHTTPErrorResponse apiError = new ApiHTTPErrorResponse(servletWebRequest.getHttpMethod(), servletWebRequest.getRequest().getServletPath(),
                HttpStatus.NOT_FOUND, dataNotFoundException.getReturnCode(), dataNotFoundException);
        //logError(apiError);
        return buildResponseEntity(apiError);
    }

    /**
     * Servlet request binding exception handler. The ServletRequestBindingException is thrown when a mandatory request
     * parameter is not sent in the request.
     * @param badRequestException {@link BadRequestException}
     * @return responseEntity with status set to 400 and payload set to {@link ApiHTTPErrorResponse}
     */
    @ExceptionHandler(value = BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiHTTPErrorResponse> handleBadRequestException(BadRequestException badRequestException,
                                                                         ServletWebRequest servletWebRequest) {

        ApiHTTPErrorResponse apiError = new ApiHTTPErrorResponse(servletWebRequest.getHttpMethod(), servletWebRequest.getRequest().getServletPath(),
                HttpStatus.BAD_REQUEST, badRequestException.getReturnCode(), badRequestException);

        return buildResponseEntity(apiError);
    }
}
