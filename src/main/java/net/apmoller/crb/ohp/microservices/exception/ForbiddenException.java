package net.apmoller.crb.ohp.microservices.exception;

/**
 * Exception class to be thrown when client is forbidden from accessing a resource
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String e) {
        super(e);
    }
}
