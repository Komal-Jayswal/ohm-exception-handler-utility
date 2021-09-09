package net.apmoller.crb.ohp.microservices.exception;

import lombok.Getter;
import net.apmoller.crb.ohp.microservices.model.ApiError;

@Getter
public class BadRequestException extends RuntimeException {
    private final transient ApiError apiError;

    public BadRequestException(ApiError apiError) {
        super(apiError.getDebugMessage());
        this.apiError = apiError;
    }

    public BadRequestException(String errorMessage) {
       super(errorMessage);
        this.apiError = null;
    }

}
