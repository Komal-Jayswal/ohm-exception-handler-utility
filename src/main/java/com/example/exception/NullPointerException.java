package com.example.exception;

import lombok.Getter;
import lombok.Setter;

/**
 * Exception class to be thrown when unexpected error encountered
 */
@Setter
@Getter
public class NullPointerException extends RuntimeException {

    String returnCode;

    public NullPointerException(String e,String returnCode) {
        super(e);
        this.returnCode = returnCode;
    }
}
