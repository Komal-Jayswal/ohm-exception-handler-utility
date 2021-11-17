package com.example.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception class to be thrown when data not found for the requested resource
 */
@Getter
@Setter
public class DataNotFoundException extends RuntimeException {

    String returnCode;

    public DataNotFoundException(String e,String returnCode) {
        super(e);
        this.returnCode = returnCode;
    }
}
