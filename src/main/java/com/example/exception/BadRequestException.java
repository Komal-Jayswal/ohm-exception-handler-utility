package com.example.exception;

import lombok.Getter;
import com.example.model.ApiHTTPErrorResponse;
import lombok.Setter;

@Getter
@Setter
public class BadRequestException extends RuntimeException {

    String returnCode;

    public BadRequestException(String e,String returnCode) {
        super(e);
        this.returnCode = returnCode;
    }

}
