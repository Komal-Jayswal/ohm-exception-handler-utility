package net.apmoller.crb.ohp.microservices.model;

import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Class representing a validation error
 */
@EqualsAndHashCode(callSuper = false)
@Data

@AllArgsConstructor
@JsonRootName("apiValidationError")
public class ApiValidationError extends ApiSubError {

    private String field;
    private Object rejectedValue;
    private String message;

}
