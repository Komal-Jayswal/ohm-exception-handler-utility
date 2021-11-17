package com.example.exception;

import com.example.model.ValidationErrorMsg;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Exception class to be thrown when data not found for the requested resource
 */

@Getter
@Setter
public class ValidationError extends RuntimeException {

    List<ValidationErrorMsg> validationErrorMsg;
    String returnCode;

    public ValidationError(List<ValidationErrorMsg> eo, String returnCode){
        super();
        this.validationErrorMsg = eo;
        this.returnCode = returnCode;
    }
    public ValidationError(){
    }
}
