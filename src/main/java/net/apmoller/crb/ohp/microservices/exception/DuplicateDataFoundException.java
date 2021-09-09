package net.apmoller.crb.ohp.microservices.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.CONFLICT)
public class DuplicateDataFoundException extends RuntimeException{

    public DuplicateDataFoundException(String exception) {
        super(exception);
    }
}
