package net.apmoller.crb.ohp.microservices.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Class representing an API sub error in order to provide additional information regarding the exception that has been
 * thrown (e.g. validation errors)
 */
@JsonDeserialize(as = ApiValidationError.class)
public abstract class ApiSubError {
}
