package net.apmoller.crb.ohp.microservices.exception;

import lombok.*;
import net.apmoller.crb.ohp.microservices.model.ApiValidationError;

@Builder(toBuilder = true)
@Getter
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class InvalidEnumException extends RuntimeException {

    ApiValidationError apiValidationError;

    @Override
    public String getMessage() {
        return "Validation failure";
    }
}
